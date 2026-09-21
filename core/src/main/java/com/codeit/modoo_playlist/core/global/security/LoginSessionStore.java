package com.codeit.modoo_playlist.core.global.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LoginSessionStore {

  void register(LoginSession session);

  Optional<LoginSession> findActive(UUID userId, UUID sid);

  void rotateRefreshToken(
      UUID userId,
      UUID sid,
      String currentRefreshTokenHash,
      String newRefreshTokenHash,
      Instant newExpiresAt
  );

  void invalidate(UUID userId, UUID sid);

  void invalidateAll(UUID userId);
}
