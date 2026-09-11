package com.codeit.modoo_playlist.modulebatch.recommendation.scoring;

import com.codeit.modoo_playlist.core.global.common.util.CosineSimilarity;
import java.time.Duration;
import java.time.Instant;

public final class TagScoreCalculator {

	private static final double HALF_LIFE_DAYS = 30.0;

	private TagScoreCalculator() {
	}

	// 최종 점수 = 감쇠 전 누적값 * 시간 감쇠 * 태그 IDF
	public static double score(double rawScore, Instant lastSignalAt, Instant now, int tagContentCount,
			long totalContentCount) {
		return rawScore * decay(lastSignalAt, now) * CosineSimilarity.idf(tagContentCount, totalContentCount);
	}

	// 시간 감쇠 = 0.5 ^ (경과일 / 반감기일수), 경과일 = (현재 - 마지막 신호 시각)을 일 단위로 환산
	public static double decay(Instant lastSignalAt, Instant now) {
		if (lastSignalAt == null) {
			return 0.0;
		}
		double days = Duration.between(lastSignalAt, now).toSeconds() / 86400.0;
		if (days <= 0) {
			return 1.0;
		}
		return Math.pow(0.5, days / HALF_LIFE_DAYS);
	}
}
