package com.codeit.modoo_playlist.infra.client.tmdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMediaSummary(
        long id,
        boolean adult,
        @JsonProperty("genre_ids") List<Integer> genreIds
) {
}
