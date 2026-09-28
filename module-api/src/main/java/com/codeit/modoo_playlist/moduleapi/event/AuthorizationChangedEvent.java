package com.codeit.modoo_playlist.moduleapi.event;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;

import java.util.UUID;

public record AuthorizationChangedEvent(
        UUID userId,
        UserRole newRole
) {
}
