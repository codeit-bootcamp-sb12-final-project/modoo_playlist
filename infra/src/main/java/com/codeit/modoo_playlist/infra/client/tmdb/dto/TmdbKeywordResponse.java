package com.codeit.modoo_playlist.infra.client.tmdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbKeywordResponse(List<Keyword> keywords, List<Keyword> results) {

    public List<Keyword> items() {
        return keywords != null ? keywords : results != null ? results : List.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Keyword(long id, String name) {
    }
}
