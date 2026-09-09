package com.codeit.modoo_playlist.moduleapi.dto.content.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ContentSummaryResponse(
        UUID id,
        String type,
        String title,
        String description,
        String thumbnailUrl,
        List<String> tags,
        BigDecimal averageRating,
        int reviewCount
) {
}
