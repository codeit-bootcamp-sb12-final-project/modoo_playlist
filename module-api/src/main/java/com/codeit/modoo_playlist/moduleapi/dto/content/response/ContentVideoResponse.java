package com.codeit.modoo_playlist.moduleapi.dto.content.response;

import java.math.BigDecimal;

import com.codeit.modoo_playlist.core.domain.content.type.VideoReleaseStatus;

public record ContentVideoResponse(
        Integer runtimeMinutes,
        String collectionName,
        String imdbId,
        VideoReleaseStatus releaseStatus,
        Integer numberOfSeasons,
        Integer numberOfEpisodes,
        String originalLanguage,
        Float popularity,
        BigDecimal externalRating,
        Integer externalRatingCount
) {
}
