package com.codeit.modoo_playlist.modulebatch.tmdb.model;

public record TmdbCandidate(long tmdbId, MediaType mediaType) {

    public enum MediaType {
        MOVIE,
        TV
    }
}
