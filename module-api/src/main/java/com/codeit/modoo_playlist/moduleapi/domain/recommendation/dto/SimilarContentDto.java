package com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto;

import java.util.UUID;

public record SimilarContentDto(
		UUID contentId,
		String title,
		String thumbnailUrl,
		double score
) {
}
