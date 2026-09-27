package com.codeit.modoo_playlist.moduleapi.dto;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
    UUID id,
    String email,
    String name,
    String profileImageUrl,
    UserRole role,
    boolean locked,
    Instant createdAt,
    Instant deletedAt,
    Instant scheduledDeletionAt
) {

  public UserDto(
      UUID id,
      String email,
      String name,
      String profileImageUrl,
      UserRole role,
      boolean locked,
      Instant createdAt
  ) {
    this(id, email, name, profileImageUrl, role, locked, createdAt, null, null);
  }
}
