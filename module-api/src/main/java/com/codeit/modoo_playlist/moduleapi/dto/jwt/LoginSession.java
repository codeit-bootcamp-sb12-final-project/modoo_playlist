package com.codeit.modoo_playlist.moduleapi.dto.jwt;

import java.time.Instant;
import java.util.UUID;

public record LoginSession(
    UUID sid,
    UUID userId,
    String refreshTokenHash,
    Instant createdAt,
    Instant expiresAt
) {

  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }
}
