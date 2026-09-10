package com.codeit.modoo_playlist.moduleapi.domain.review.service;

import com.codeit.modoo_playlist.core.domain.review.entity.Review;
import java.math.BigDecimal;
import java.util.UUID;

public interface ReviewService {

    UUID createReview(UUID contentId, UUID authorId, String text, BigDecimal rating);

    void updateReview(UUID reviewId, UUID authorId, String text, BigDecimal rating);

    void deleteReview(UUID reviewId, UUID authorId);

    Review getReview(UUID reviewId);

}