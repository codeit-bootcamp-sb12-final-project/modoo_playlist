package com.codeit.modoo_playlist.moduleapi.domain.review.repository.query;

import com.codeit.modoo_playlist.core.domain.review.entity.Review;

import java.util.List;
import java.util.UUID;

public record ReviewQueryPage(
        List<Review> reviews,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount
) {
    public ReviewQueryPage {
        reviews = List.copyOf(reviews);
    }
}