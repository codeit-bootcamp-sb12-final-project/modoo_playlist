package com.codeit.modoo_playlist.modulebatch.recommendation.model;

import java.time.Instant;

public record PreferenceTagRow(
		String userId,
		String tagId,
		double rawScore,
		double score,
		Instant lastSignalAt
) {
}
