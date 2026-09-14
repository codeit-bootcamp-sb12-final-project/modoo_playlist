package com.codeit.modoo_playlist.modulebatch.embedding.model;

public record ContentEmbeddingTarget(
    String contentId,
    String title,
    String description,
    String tagNames,
    String currentSourceHash
) {

}
