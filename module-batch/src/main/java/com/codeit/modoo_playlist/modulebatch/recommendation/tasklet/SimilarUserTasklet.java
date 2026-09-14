package com.codeit.modoo_playlist.modulebatch.recommendation.tasklet;

import com.codeit.modoo_playlist.core.global.common.util.CosineSimilarity;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.SimilarityRow;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.TagName;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.UserTagScore;
import com.codeit.modoo_playlist.modulebatch.recommendation.persistence.RecommendationRecalcMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class SimilarUserTasklet implements Tasklet {

  private static final int TOP_K = 20;
  private static final int SHARED_TAG_LIMIT = 3;
  private static final int UPSERT_CHUNK_SIZE = 1000;

  private final RecommendationRecalcMapper mapper;

  @Override
  public @Nullable RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    mapper.deleteAllSimilarities();
    Map<String, Map<String, Double>> vectorsByUser = mapper.findAllPreferenceScores().stream()
        .collect(Collectors.groupingBy(
            UserTagScore::userId,
            Collectors.toMap(UserTagScore::tagId, UserTagScore::score)
        ));
    if (vectorsByUser.size() < 2) {
      log.info("유사 사용자 재계산: 비교 대상 사용자 부족");
      return RepeatStatus.FINISHED;
    }
    Map<String, String> tagNames = mapper.findTagNames().stream()
        .collect(Collectors.toMap(TagName::tagId, TagName::name));
    Instant now = Instant.now();

    List<String> userIds = new ArrayList<>(vectorsByUser.keySet());
    Map<String, SimilarityRow> rowsByKey = new HashMap<>();

    for (String userId : userIds) {
      Map<String, Double> vector = vectorsByUser.get(userId);
      List<Map.Entry<String, Double>> topNeighbors = userIds.stream()
          .filter(otherId -> !otherId.equals(userId))
          .map(otherId -> Map.entry(otherId,
              CosineSimilarity.compute(vector, vectorsByUser.get(otherId))))
          .filter(entry -> entry.getValue() > 0)
          .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
          .limit(TOP_K)
          .toList();

      for (Map.Entry<String, Double> neighbor : topNeighbors) {
        String otherId = neighbor.getKey();
        double score = neighbor.getValue();
        String sharedTags = sharedTagNames(vector, vectorsByUser.get(otherId), tagNames);

        rowsByKey.put(userId + ":" + otherId,
            new SimilarityRow(userId, otherId, score, sharedTags, now));
        rowsByKey.put(otherId + ":" + userId,
            new SimilarityRow(otherId, userId, score, sharedTags, now));
      }
    }
    List<SimilarityRow> rows = new ArrayList<>(rowsByKey.values());
    for (int i = 0; i < rows.size(); i += UPSERT_CHUNK_SIZE) {
      mapper.upsertSimilarities(rows.subList(i, Math.min(i + UPSERT_CHUNK_SIZE, rows.size())));
    }
    log.info("유사 사용자 재계산 완료: {}건", rows.size());
    return RepeatStatus.FINISHED;
  }

  private String sharedTagNames(Map<String, Double> a, Map<String, Double> b,
      Map<String, String> tagNames) {
    return a.entrySet().stream()
        .filter(entry -> b.containsKey(entry.getKey()))
        .sorted(Comparator.<Map.Entry<String, Double>>comparingDouble(
                entry -> entry.getValue() * b.get(entry.getKey())).reversed()
            .thenComparing(Map.Entry::getKey))
        .limit(SHARED_TAG_LIMIT)
        .map(entry -> tagNames.getOrDefault(entry.getKey(), entry.getKey()))
        .collect(Collectors.joining(", "));
  }
}
