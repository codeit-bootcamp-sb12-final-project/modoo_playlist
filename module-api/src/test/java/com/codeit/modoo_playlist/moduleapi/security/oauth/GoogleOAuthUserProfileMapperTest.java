package com.codeit.modoo_playlist.moduleapi.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import com.codeit.modoo_playlist.moduleapi.security.CodedAuthenticationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoogleOAuthUserProfileMapperTest {

  private final GoogleOAuthUserProfileMapper mapper = new GoogleOAuthUserProfileMapper();

  @Test
  void mapsVerifiedGoogleClaims() {
    OAuthUserProfile profile = mapper.map(Map.of(
        "sub", "google-user-id",
        "email", "USER@Example.com",
        "email_verified", true,
        "name", "Google User",
        "picture", "https://example.com/profile.png"
    ));

    assertThat(profile.provider()).isEqualTo(Provider.GOOGLE);
    assertThat(profile.providerUserId()).isEqualTo("google-user-id");
    assertThat(profile.email()).isEqualTo("user@example.com");
    assertThat(profile.name()).isEqualTo("Google User");
    assertThat(profile.profileImageUrl()).isEqualTo("https://example.com/profile.png");
  }

  @Test
  void fallsBackToEmailPrefixWhenNameIsMissing() {
    OAuthUserProfile profile = mapper.map(Map.of(
        "sub", "google-user-id",
        "email", "user@example.com",
        "email_verified", true
    ));

    assertThat(profile.name()).isEqualTo("user");
    assertThat(profile.profileImageUrl()).isNull();
  }

  @Test
  void rejectsUnverifiedEmail() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "google-user-id");
    claims.put("email", "user@example.com");
    claims.put("email_verified", false);

    assertThatThrownBy(() -> mapper.map(claims))
        .isInstanceOfSatisfying(CodedAuthenticationException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS)
        );
  }

  @Test
  void rejectsMissingSubject() {
    assertThatThrownBy(() -> mapper.map(Map.of(
        "email", "user@example.com",
        "email_verified", true
    )))
        .isInstanceOf(CodedAuthenticationException.class);
  }
}
