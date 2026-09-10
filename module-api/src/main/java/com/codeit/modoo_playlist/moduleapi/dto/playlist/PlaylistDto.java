package com.codeit.modoo_playlist.moduleapi.dto.playlist;

import com.codeit.modoo_playlist.core.domain.playlist.entity.GeneratedBy;

import java.time.Instant;
import java.util.UUID;

public record PlaylistDto(
        UUID id,
        UUID ownerId,
        String title,
        String description,
        GeneratedBy generatedBy,
        Instant createdAt
) {
}