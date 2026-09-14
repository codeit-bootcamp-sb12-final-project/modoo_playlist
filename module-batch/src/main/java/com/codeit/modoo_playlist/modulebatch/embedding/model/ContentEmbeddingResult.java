package com.codeit.modoo_playlist.modulebatch.embedding.model;

public record ContentEmbeddingResult(
    String contentId,
    String vector,
    int dims,
    String model,
    String sourceHash
) {

}
