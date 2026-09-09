package com.codeit.modoo_playlist.moduleapi.dto.content.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ContentDetailResponse(
        UUID id,
        String type,
        String title,
        String description,
        String thumbnailUrl,
        List<String> tags,
        BigDecimal averageRating,
        int reviewCount,
        long watcherCount,
        LocalDate releaseDate,
        String originCountry,
        ContentVideoResponse video,
        ContentSportsResponse sports,
        List<ContentPersonResponse> people
) {
}
