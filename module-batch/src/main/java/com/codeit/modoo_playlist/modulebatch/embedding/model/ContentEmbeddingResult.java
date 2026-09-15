package com.codeit.modoo_playlist.modulebatch.embedding.model;

public record ContentEmbeddingResult(
    String contentId,
    String text,
    String title,
    String thumbnailUrl,
    String sourceHash
) {

}
