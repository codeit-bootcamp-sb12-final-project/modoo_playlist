package com.codeit.modoo_playlist.moduleapi.dto.content.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ContentListItemResponse(
        UUID id,
        String type,
        String title,
        String description,
        String thumbnailUrl,
        LocalDate releaseDate,
        List<String> tags,
        BigDecimal averageRating,
        int reviewCount,
        long watcherCount
) {
}
