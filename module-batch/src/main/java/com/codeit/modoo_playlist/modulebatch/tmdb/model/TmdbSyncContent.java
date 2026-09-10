package com.codeit.modoo_playlist.modulebatch.tmdb.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TmdbSyncContent(
        String id,
        String type,
        String title,
        String description,
        String thumbnailUrl,
        String sourceId,
        LocalDate releaseDate,
        String originCountry,
        Video video,
        List<Person> people,
        List<Tag> tags,
        boolean replacePeople,
        boolean replaceTags
) {
    public record Video(
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
    }

    public record Person(
            String id,
            String roleType,
            String personName,
            String characterName,
            int displayOrder,
            String personId,
            String personImg
    ) {
    }

    public record Tag(String id, String name, String kind) {
    }
}
