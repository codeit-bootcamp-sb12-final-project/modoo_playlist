package com.codeit.modoo_playlist.infra.client.tmdb.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbTvDetailResponse(
        long id,
        String name,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("first_air_date") LocalDate firstAirDate,
        @JsonProperty("origin_country") List<String> originCountry,
        @JsonProperty("original_language") String originalLanguage,
        Float popularity,
        @JsonProperty("vote_average") BigDecimal voteAverage,
        @JsonProperty("vote_count") Integer voteCount,
        @JsonProperty("episode_run_time") List<Integer> episodeRunTime,
        @JsonProperty("number_of_seasons") Integer numberOfSeasons,
        @JsonProperty("number_of_episodes") Integer numberOfEpisodes,
        String status,
        List<TmdbGenreResponse> genres,
        @JsonProperty("created_by") List<Creator> createdBy,
        TmdbCreditsResponse credits,
        TmdbKeywordResponse keywords,
        @JsonProperty("external_ids") ExternalIds externalIds
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Creator(long id, String name, @JsonProperty("profile_path") String profilePath) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExternalIds(@JsonProperty("imdb_id") String imdbId) {
    }
}
