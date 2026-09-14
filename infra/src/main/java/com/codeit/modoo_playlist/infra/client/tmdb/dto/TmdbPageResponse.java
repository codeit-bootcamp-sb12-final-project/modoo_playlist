package com.codeit.modoo_playlist.infra.client.tmdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbPageResponse<T>(
        List<T> results
) {
}
