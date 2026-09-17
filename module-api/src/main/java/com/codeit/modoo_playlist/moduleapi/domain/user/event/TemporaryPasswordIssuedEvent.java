package com.codeit.modoo_playlist.moduleapi.domain.user.event;

import java.time.Instant;
import java.util.UUID;

public record TemporaryPasswordIssuedEvent(
    UUID userId,
    String email,
    String temporaryPassword,
    Instant expiresAt
) {

}
