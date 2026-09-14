package com.codeit.modoo_playlist.modulebatch.recommendation.model;

import java.time.Instant;

public record SimilarityRow(
    String userId,
    String otherUserId,
    double score,
    String sharedTags,
    Instant computedAt
) {

}
