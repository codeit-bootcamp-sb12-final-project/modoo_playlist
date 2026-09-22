package com.codeit.modoo_playlist.core.domain.user.dto;

import java.util.UUID;

public record UserSummaryResponse(
        UUID userId,
        String name,
        String profileImageUrl
) {
}