package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.impl;

import com.codeit.modoo_playlist.core.global.common.util.CosineSimilarity;
import com.codeit.modoo_playlist.core.global.common.util.ScoringEngine;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.interaction.repository.UserContentInteractionRepository;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.SimilarUserDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.repository.UserSimilarityRepository;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.SimilarUserInteractionProjection;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.RecommendationService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {

  private static final int CANDIDATE_POOL_SIZE = 50;
  private static final int SIMILAR_USER_POOL_SIZE = 20;
  private static final int CANDIDATE_CONTENT_POOL_SIZE = 2000;

  private final ContentTagRepository contentTagRepository;
  private final ContentRepository contentRepository;
  private final UserSimilarityRepository userSimilarityRepository;
  private final UserContentInteractionRepository userContentInteractionRepository;

  @Override
  public List<RecommendedContentDto> getSimilarContents(UUID contentId, Integer limit) {
    List<RecommendedContentDto> candidates =
        contentTagRepository.findSimilarContents(contentId, PageRequest.of(0, CANDIDATE_POOL_SIZE));
    if (candidates.isEmpty()) {
      return List.of();
    }

    List<UUID> ids = new ArrayList<>(candidates.stream().map(RecommendedContentDto::contentId).toList());
    ids.add(contentId);

    long totalContentCount = contentRepository.count();
    Map<UUID, Map<UUID, Double>> weightsByContent = contentTagRepository.findAllWithTagByContentIds(ids).stream()
        .collect(Collectors.groupingBy(
            ct -> ct.getId().getContentId(),
            Collectors.toMap(
                ct -> ct.getTag().getId(),
                ct -> CosineSimilarity.idf(ct.getTag().getContentCount(), totalContentCount)
            )
        ));

    Map<UUID, Double> target = weightsByContent.getOrDefault(contentId, Map.of());

    return candidates.stream()
        .map(c -> new RecommendedContentDto(
            c.contentId(), c.title(), c.thumbnailUrl(),
            CosineSimilarity.compute(target, weightsByContent.getOrDefault(c.contentId(), Map.of()))
        ))
        .sorted(Comparator.comparingDouble(RecommendedContentDto::score).reversed())
        .limit(limit)
        .toList();
  }

  @Override
  public List<RecommendedContentDto> getRecommendationsForMe(UUID userId, Integer limit) {
    List<SimilarUserDto> similarUsers =
        userSimilarityRepository.findTopSimilarUsersByUserId(userId, PageRequest.of(0, SIMILAR_USER_POOL_SIZE));
    if (similarUsers.isEmpty()) {
      return List.of();
    }

    Map<UUID, Double> similarityByUser = similarUsers.stream()
        .collect(Collectors.toMap(SimilarUserDto::userId, u -> u.score().doubleValue()));

    List<UUID> similarUserIds = new ArrayList<>(similarityByUser.keySet());
    List<UUID> candidateContentIds = userContentInteractionRepository.findCandidateContentIds(
        userId, similarUserIds, PageRequest.of(0, CANDIDATE_CONTENT_POOL_SIZE));
    if (candidateContentIds.isEmpty()) {
      return List.of();
    }

    List<SimilarUserInteractionProjection> candidates = userContentInteractionRepository
        .findInteractionsByContentIds(similarUserIds, candidateContentIds);

    Map<UUID, Double> scoreByContent = new LinkedHashMap<>();
    Map<UUID, SimilarUserInteractionProjection> firstSeenByContent = new LinkedHashMap<>();
    for (SimilarUserInteractionProjection c : candidates) {
      double contribution = similarityByUser.getOrDefault(c.otherUserId(), 0.0)
          * ScoringEngine.weight(c.type(), c.value(), c.occurrenceCount());
      scoreByContent.merge(c.contentId(), contribution, Double::sum);
      firstSeenByContent.putIfAbsent(c.contentId(), c);
    }

    return scoreByContent.entrySet().stream()
        .filter(e -> e.getValue() > 0)
        .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
        .limit(limit)
        .map(e -> {
          SimilarUserInteractionProjection c = firstSeenByContent.get(e.getKey());
          return new RecommendedContentDto(c.contentId(), c.title(), c.thumbnailUrl(), e.getValue());
        })
        .toList();
  }
}
