package com.codeit.modoo_playlist.infra.client.tmdb.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovieDetailResponse(
        long id,
        String title,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") LocalDate releaseDate,
        @JsonProperty("origin_country") List<String> originCountry,
        @JsonProperty("original_language") String originalLanguage,
        Float popularity,
        @JsonProperty("vote_average") BigDecimal voteAverage,
        @JsonProperty("vote_count") Integer voteCount,
        Integer runtime,
        @JsonProperty("imdb_id") String imdbId,
        String status,
        @JsonProperty("belongs_to_collection") CollectionInfo belongsToCollection,
        List<TmdbGenreResponse> genres,
        TmdbCreditsResponse credits,
        TmdbKeywordResponse keywords
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CollectionInfo(long id, String name) {
    }
}
