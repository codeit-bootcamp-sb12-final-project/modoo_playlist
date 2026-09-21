package com.codeit.modoo_playlist.modulebatch.embedding.model;

import lombok.Builder;

@Builder
public record ContentEmbeddingTarget(
    String contentId,
    String title,
    String description,
    String thumbnailUrl,
    String tagNames,
    String currentSourceHash,
    String type,
    Integer releaseYear,
    String originCountry,
    String directors,
    String actors,
    String sportType,
    String league,
    String season,
    String homeTeam,
    String awayTeam,
    String venue
) {

}
