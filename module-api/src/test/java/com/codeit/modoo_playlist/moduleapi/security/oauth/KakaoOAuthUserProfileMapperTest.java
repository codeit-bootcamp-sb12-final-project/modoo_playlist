package com.codeit.modoo_playlist.moduleapi.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import com.codeit.modoo_playlist.moduleapi.security.CodedAuthenticationException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KakaoOAuthUserProfileMapperTest {

  private final KakaoOAuthUserProfileMapper mapper = new KakaoOAuthUserProfileMapper();

  @Test
  void mapsVerifiedKakaoOidcClaims() {
    OAuthUserProfile profile = mapper.map(Map.of(
        "sub", "kakao-user-id",
        "email", "USER@Example.com",
        "email_verified", true,
        "nickname", "Kakao User",
        "picture", "https://example.com/profile.png"
    ));

    assertThat(profile.provider()).isEqualTo(Provider.KAKAO);
    assertThat(profile.providerUserId()).isEqualTo("kakao-user-id");
    assertThat(profile.email()).isEqualTo("user@example.com");
    assertThat(profile.name()).isEqualTo("Kakao User");
    assertThat(profile.profileImageUrl()).isEqualTo("https://example.com/profile.png");
  }

  @Test
  void fallsBackToEmailPrefixWhenNicknameIsMissing() {
    OAuthUserProfile profile = mapper.map(Map.of(
        "sub", "kakao-user-id",
        "email", "user@example.com",
        "email_verified", true
    ));

    assertThat(profile.name()).isEqualTo("user");
    assertThat(profile.profileImageUrl()).isNull();
  }

  @Test
  void rejectsUnverifiedEmail() {
    assertThatThrownBy(() -> mapper.map(Map.of(
        "sub", "kakao-user-id",
        "email", "user@example.com",
        "email_verified", false
    )))
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
