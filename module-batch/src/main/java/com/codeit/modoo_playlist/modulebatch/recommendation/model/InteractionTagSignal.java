package com.codeit.modoo_playlist.modulebatch.recommendation.model;

import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import java.math.BigDecimal;
import java.time.Instant;

public record InteractionTagSignal(
		String userId,
		String tagId,
		InteractionType type,
		BigDecimal value,
		int occurrenceCount,
		Instant updatedAt
) {
}
