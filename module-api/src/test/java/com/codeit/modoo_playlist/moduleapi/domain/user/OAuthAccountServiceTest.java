package com.codeit.modoo_playlist.moduleapi.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.domain.user.entity.SocialAccount;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.SocialAccountRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.OAuthAccountServiceImpl;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthAccountResult;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuthAccountServiceTest {

  private static final String PROVIDER_USER_ID = "google-user-id";
  private static final String EMAIL = "oauth@example.com";

  @Mock
  SocialAccountRepository socialAccounts;

  @Mock
  UserRepository users;

  private OAuthAccountServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new OAuthAccountServiceImpl(socialAccounts, users);
  }

  @Test
  @DisplayName("resolves an existing social account")
  void resolvesExistingSocialAccount() {
    UUID userId = UUID.randomUUID();
    User user = User.createOAuth(EMAIL, "OAuth User", null);
    ReflectionTestUtils.setField(user, "id", userId);
    SocialAccount socialAccount = SocialAccount.create(
        user,
        Provider.GOOGLE,
        PROVIDER_USER_ID
    );
    when(socialAccounts.findByProviderAndProviderUserId(
        Provider.GOOGLE,
        PROVIDER_USER_ID
    )).thenReturn(Optional.of(socialAccount));

    OAuthAccountResult result = service.resolveOrCreate(profile());

    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.newlyRegistered()).isFalse();
    verify(users, never()).save(any());
    verify(socialAccounts, never()).save(any());
  }

  @Test
  @DisplayName("rejects an OAuth login for a withdrawn social account")
  void rejectsWithdrawnSocialAccount() {
    User user = User.createOAuth(EMAIL, "OAuth User", null);
    user.withdraw(Instant.now());
    SocialAccount socialAccount = SocialAccount.create(
        user,
        Provider.GOOGLE,
        PROVIDER_USER_ID
    );
    when(socialAccounts.findByProviderAndProviderUserId(
        Provider.GOOGLE,
        PROVIDER_USER_ID
    )).thenReturn(Optional.of(socialAccount));

    assertThatThrownBy(() -> service.resolveOrCreate(profile()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_ACCOUNT_WITHDRAWN)
        );

    verify(users, never()).save(any());
    verify(socialAccounts, never()).save(any());
  }

  @Test
  @DisplayName("creates a user and social account for a first login")
  void createsUserAndSocialAccount() {
    UUID userId = UUID.randomUUID();
    when(socialAccounts.findByProviderAndProviderUserId(
        Provider.GOOGLE,
        PROVIDER_USER_ID
    )).thenReturn(Optional.empty());
    when(users.existsByEmail(EMAIL)).thenReturn(false);
    when(users.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      ReflectionTestUtils.setField(user, "id", userId);
      return user;
    });

    OAuthAccountResult result = service.resolveOrCreate(profile());

    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.newlyRegistered()).isTrue();
    verify(users).save(assertArg(user -> {
      assertThat(user.getEmail()).isEqualTo(EMAIL);
      assertThat(user.getUsername()).isEqualTo("OAuth User");
      assertThat(user.getPassword()).isNull();
      assertThat(user.getProfileImageUrl()).isEqualTo("profile.png");
    }));
    verify(socialAccounts).save(assertArg(socialAccount -> {
      assertThat(socialAccount.getUser().getId()).isEqualTo(userId);
      assertThat(socialAccount.getProvider()).isEqualTo(Provider.GOOGLE);
      assertThat(socialAccount.getProviderUserId()).isEqualTo(PROVIDER_USER_ID);
    }));
  }

  @Test
  @DisplayName("does not automatically link an existing email")
  void rejectsExistingEmail() {
    when(socialAccounts.findByProviderAndProviderUserId(
        Provider.GOOGLE,
        PROVIDER_USER_ID
    )).thenReturn(Optional.empty());
    when(users.existsByEmail(EMAIL)).thenReturn(true);

    assertThatThrownBy(() -> service.resolveOrCreate(profile()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS)
        );

    verify(users, never()).save(any());
    verify(socialAccounts, never()).save(any());
  }

  private OAuthUserProfile profile() {
    return new OAuthUserProfile(
        Provider.GOOGLE,
        PROVIDER_USER_ID,
        EMAIL,
        "OAuth User",
        "profile.png"
    );
  }
}
