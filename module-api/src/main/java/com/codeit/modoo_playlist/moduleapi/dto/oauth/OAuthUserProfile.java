package com.codeit.modoo_playlist.moduleapi.dto.oauth;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import java.util.Objects;

public record OAuthUserProfile(
    Provider provider,
    String providerUserId,
    String email,
    String name,
    String profileImageUrl
) {

  public OAuthUserProfile {
    Objects.requireNonNull(provider, "OAuth provider is required");

    if (provider == Provider.LOCAL) {
      throw new IllegalArgumentException("LOCAL cannot be used as an OAuth provider");
    }
    requireText(providerUserId, "OAuth provider user ID is required");
    requireText(email, "OAuth email is required");
    requireText(name, "OAuth user name is required");
  }

  private static void requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }
}
