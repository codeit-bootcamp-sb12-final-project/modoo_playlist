package com.codeit.modoo_playlist.modulebatch.recommendation.model;

public record UserTagScore(
    String userId,
    String tagId,
    double score
) {

}
