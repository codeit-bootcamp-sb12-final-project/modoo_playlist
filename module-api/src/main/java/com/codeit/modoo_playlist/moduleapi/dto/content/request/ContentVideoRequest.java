package com.codeit.modoo_playlist.moduleapi.dto.content.request;

import java.math.BigDecimal;

import com.codeit.modoo_playlist.core.domain.content.type.VideoReleaseStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ContentVideoRequest(
        @PositiveOrZero Integer runtimeMinutes,
        @Size(max = 100) String collectionName,
        @Size(max = 20) String imdbId,
        VideoReleaseStatus releaseStatus,
        @PositiveOrZero Integer numberOfSeasons,
        @PositiveOrZero Integer numberOfEpisodes,
        @Size(max = 10) String originalLanguage,
        @PositiveOrZero Float popularity,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal externalRating,
        @PositiveOrZero Integer externalRatingCount
) {
}
