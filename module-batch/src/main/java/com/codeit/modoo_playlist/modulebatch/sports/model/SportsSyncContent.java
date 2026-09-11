package com.codeit.modoo_playlist.modulebatch.sports.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SportsSyncContent(
        String id,
        String title,
        String description,
        String thumbnailUrl,
        String sourceId,
        LocalDate releaseDate,
        String originCountry,
        Sports sports,
        List<Tag> tags
) {
    public record Sports(
            String sportType,
            String league,
            String season,
            String homeTeam,
            String awayTeam,
            String venue,
            String status,
            Instant kickoffAt
    ) {
    }

    public record Tag(String id, String name, String kind) {
    }
}
