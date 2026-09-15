package com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto;

import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import java.math.BigDecimal;
import java.util.UUID;

public record SimilarUserInteractionProjection(
		UUID contentId,
		String title,
		String thumbnailUrl,
		UUID otherUserId,
		InteractionType type,
		BigDecimal value,
		Integer occurrenceCount
) {
}
