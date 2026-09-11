package com.codeit.modoo_playlist.moduleapi.dto.user.response;

import java.util.UUID;

public record UserSummaryResponse(
        UUID userId,
        String name,
        String profileImageUrl
) {
}