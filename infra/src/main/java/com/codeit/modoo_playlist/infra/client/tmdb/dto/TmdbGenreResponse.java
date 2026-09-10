package com.codeit.modoo_playlist.infra.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbGenreResponse(long id, String name) {
}
