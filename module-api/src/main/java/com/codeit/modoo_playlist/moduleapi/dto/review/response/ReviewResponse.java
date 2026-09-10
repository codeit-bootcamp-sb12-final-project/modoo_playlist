package com.codeit.modoo_playlist.moduleapi.dto.review.response;

import com.codeit.modoo_playlist.core.domain.review.entity.ReviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID contentId,
        UUID authorId,
        String text,
        BigDecimal rating,
        ReviewStatus status,
        Instant createdAt
) {
}