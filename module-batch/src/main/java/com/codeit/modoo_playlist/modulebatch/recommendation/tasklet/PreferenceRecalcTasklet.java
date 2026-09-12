package com.codeit.modoo_playlist.modulebatch.recommendation.tasklet;

import com.codeit.modoo_playlist.modulebatch.recommendation.model.InteractionTagSignal;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.PreferenceTagRow;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.TagContentCount;
import com.codeit.modoo_playlist.modulebatch.recommendation.persistence.RecommendationRecalcMapper;
import com.codeit.modoo_playlist.modulebatch.recommendation.scoring.ScoringEngine;
import com.codeit.modoo_playlist.modulebatch.recommendation.scoring.TagScoreCalculator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class PreferenceRecalcTasklet implements Tasklet {

	private static final int UPSERT_CHUNK_SIZE = 1000;

	private final RecommendationRecalcMapper mapper;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		List<InteractionTagSignal> signals = mapper.findInteractionTagSignals();
		if (signals.isEmpty()) {
			log.info("취향 재계산: 대상 상호작용 없음");
			return RepeatStatus.FINISHED;
		}

		Map<String, Integer> tagContentCounts = mapper.findTagContentCounts().stream()
				.collect(Collectors.toMap(TagContentCount::tagId, TagContentCount::contentCount));
		long totalContentCount = mapper.countActiveContents();
		Instant now = Instant.now();

		List<PreferenceTagRow> rows = signals.stream()
				.collect(Collectors.groupingBy(s -> s.userId() + ":" + s.tagId()))
				.values().stream()
				.map(group -> toRow(group, tagContentCounts, totalContentCount, now))
				.toList();

		for (int i = 0; i < rows.size(); i += UPSERT_CHUNK_SIZE) {
			mapper.upsertPreferenceTags(rows.subList(i, Math.min(i + UPSERT_CHUNK_SIZE, rows.size())));
		}
		log.info("취향 재계산 완료: {}건", rows.size());
		return RepeatStatus.FINISHED;
	}

	private PreferenceTagRow toRow(
			List<InteractionTagSignal> group,
			Map<String, Integer> tagContentCounts,
			long totalContentCount,
			Instant now
	) {
		InteractionTagSignal first = group.get(0);

		double rawScore = group.stream()
				.mapToDouble(s -> ScoringEngine.weight(s.type(), s.value(), s.occurrenceCount()))
				.sum();

		Instant lastSignalAt = group.stream()
				.map(InteractionTagSignal::updatedAt)
				.max(Instant::compareTo)
				.orElse(now);

		int contentCount = tagContentCounts.getOrDefault(first.tagId(), 0);
		double score = TagScoreCalculator.score(rawScore, lastSignalAt, now, contentCount, totalContentCount);

		return new PreferenceTagRow(first.userId(), first.tagId(), rawScore, score, lastSignalAt);
	}
}
