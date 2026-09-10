package com.codeit.modoo_playlist.infra.client.tmdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbCreditsResponse(List<Cast> cast, List<Crew> crew) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cast(
            long id,
            String name,
            String character,
            int order,
            @JsonProperty("profile_path") String profilePath
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Crew(
            long id,
            String name,
            String job,
            String department,
            @JsonProperty("profile_path") String profilePath
    ) {
    }
}
