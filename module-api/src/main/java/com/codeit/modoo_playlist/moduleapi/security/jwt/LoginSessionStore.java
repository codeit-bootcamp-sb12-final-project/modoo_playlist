package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginSession;
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
}
