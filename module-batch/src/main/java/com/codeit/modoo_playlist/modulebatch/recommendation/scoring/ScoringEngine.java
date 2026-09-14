package com.codeit.modoo_playlist.modulebatch.recommendation.scoring;

import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import java.math.BigDecimal;

public final class ScoringEngine {

	private static final int MAX_OCCURRENCE = 5;

	private ScoringEngine() {
	}

	// 가중치 = 타입별 기본 가중치 * min(발생 횟수, 상한)
	public static double weight(InteractionType type, BigDecimal value, int occurrenceCount) {
		return baseWeight(type, value) * Math.min(occurrenceCount, MAX_OCCURRENCE);
	}

	private static double baseWeight(InteractionType type, BigDecimal value) {
		return switch (type) {
			case LIKE -> 3;
			case DISLIKE -> -3;
			case NOT_INTERESTED -> -2;
			case PLAYLIST_ADD -> 2;
			case MARK_WATCHED -> 2;
			case REVIEW_WRITE -> reviewWeight(value);
			case WATCH_SESSION -> watchSessionWeight(value);
			case VIEW, TRAILER_WATCH -> 0;
		};
	}

	private static double reviewWeight(BigDecimal value) {
		if (value == null) {
			return 0;
		}
		double rating = value.doubleValue();
		if (rating >= 4) {
			return 3;
		}
		if (rating <= 2) {
			return -3;
		}
		return 0;
	}

	private static double watchSessionWeight(BigDecimal value) {
		if (value == null) {
			return 0;
		}
		double seconds = value.doubleValue();
		if (seconds >= 3600) {
			return 3;
		}
		if (seconds < 300) {
			return -1;
		}
		return 0;
	}
}
