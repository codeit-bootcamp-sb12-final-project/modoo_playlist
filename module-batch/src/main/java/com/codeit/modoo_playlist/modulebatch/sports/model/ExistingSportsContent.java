package com.codeit.modoo_playlist.modulebatch.sports.model;

import java.time.Instant;
import java.time.LocalDate;

public record ExistingSportsContent(
        String id,
        String title,
        String description,
        String thumbnailUrl,
        Instant deletedAt,
        LocalDate releaseDate,
        String originCountry,
        String sportType,
        String league,
        String season,
        String homeTeam,
        String awayTeam,
        String venue,
        String status,
        Instant kickoffAt
) {
    public record Tag(String name, String source) {
    }
}
