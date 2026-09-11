package com.codeit.modoo_playlist.modulebatch.tmdb.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record ExistingTmdbContent(
        String id,
        String title,
        String description,
        String thumbnailUrl,
        Instant deletedAt,
        LocalDate releaseDate,
        String originCountry,
        Integer runtimeMinutes,
        String collectionName,
        String imdbId,
        String releaseStatus,
        Integer numberOfSeasons,
        Integer numberOfEpisodes,
        String originalLanguage,
        Float popularity,
        BigDecimal externalRating,
        Integer externalRatingCount
) {

    public record Person(
            String roleType,
            String personName,
            String characterName,
            int displayOrder,
            String personId,
            String personImg
    ) {
    }

    public record Tag(String name, String source) {
    }
}
