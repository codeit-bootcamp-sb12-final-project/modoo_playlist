package com.codeit.modoo_playlist.moduleapi.dto;

import java.util.UUID;

public record UserSummary(
        UUID userId,
        String name,
        String profileImageUrl
) {
}
