package com.codeit.modoo_playlist.moduleapi.dto.review.response;

import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID contentId,
        UserSummaryResponse author,
        String text,
        BigDecimal rating
) {
}