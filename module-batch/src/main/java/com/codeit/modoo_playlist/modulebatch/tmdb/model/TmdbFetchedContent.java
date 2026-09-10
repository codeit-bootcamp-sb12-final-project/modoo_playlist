package com.codeit.modoo_playlist.modulebatch.tmdb.model;

import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbMovieDetailResponse;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbTvDetailResponse;

public record TmdbFetchedContent(
        TmdbCandidate candidate,
        TmdbMovieDetailResponse movieDetail,
        TmdbTvDetailResponse tvDetail,
        RuntimeException failure
) {

    public static TmdbFetchedContent movie(TmdbCandidate candidate, TmdbMovieDetailResponse detail) {
        return new TmdbFetchedContent(candidate, detail, null, null);
    }

    public static TmdbFetchedContent tv(TmdbCandidate candidate, TmdbTvDetailResponse detail) {
        return new TmdbFetchedContent(candidate, null, detail, null);
    }

    public static TmdbFetchedContent failed(TmdbCandidate candidate, RuntimeException failure) {
        return new TmdbFetchedContent(candidate, null, null, failure);
    }
}
