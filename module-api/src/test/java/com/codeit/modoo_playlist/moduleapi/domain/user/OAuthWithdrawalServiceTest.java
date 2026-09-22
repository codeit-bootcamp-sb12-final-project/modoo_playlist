package com.codeit.modoo_playlist.moduleapi.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.domain.user.entity.SocialAccount;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.SocialAccountRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.SocialAccountUnlinkClient;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.OAuthWithdrawalServiceImpl;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import com.codeit.modoo_playlist.core.global.security.LoginSessionStore;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequest;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequestStore;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthWithdrawalServiceTest {

  private static final String EMAIL = "user@example.com";

  @Mock UserRepository userRepository;
  @Mock SocialAccountRepository socialAccountRepository;
  @Mock OAuthWithdrawalRequestStore requestStore;
  @Mock SocialAccountUnlinkClient unlinkClient;
  @Mock LoginSessionStore loginSessionStore;
  @Mock User user;
  @Mock SocialAccount socialAccount;

  private OAuthWithdrawalServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new OAuthWithdrawalServiceImpl(
        userRepository,
        socialAccountRepository,
        requestStore,
        unlinkClient,
        loginSessionStore
    );
  }

  @Test
  void preparesAuthorizationUrlFromStoredSocialAccount() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(socialAccountRepository.findByUserId(userId)).thenReturn(Optional.of(socialAccount));
    when(socialAccount.getProvider()).thenReturn(Provider.GOOGLE);
    when(socialAccount.getProviderUserId()).thenReturn("google-sub");
    when(requestStore.create(any(OAuthWithdrawalRequest.class))).thenReturn("request-id");

    var response = service.prepare(userId);

    assertThat(response.authorizationUrl())
        .isEqualTo("/oauth2/authorization/google?withdrawalRequestId=request-id");
    verify(requestStore).create(new OAuthWithdrawalRequest(userId, Provider.GOOGLE, "google-sub"));
  }

  @Test
  void unlinksMatchingAccountThenWithdrawsAndInvalidatesSession() {
    UUID userId = UUID.randomUUID();
    OAuthWithdrawalRequest request =
        new OAuthWithdrawalRequest(userId, Provider.KAKAO, "kakao-sub");
    OAuthUserProfile profile =
        new OAuthUserProfile(Provider.KAKAO, "kakao-sub", "user@example.com", "user", null);
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(socialAccountRepository.findByUserId(userId)).thenReturn(Optional.of(socialAccount));
    when(socialAccount.getProvider()).thenReturn(Provider.KAKAO);
    when(socialAccount.getProviderUserId()).thenReturn("kakao-sub");

    service.complete(request, profile, "provider-access-token");

    verify(unlinkClient).unlink(Provider.KAKAO, "provider-access-token");
    verify(user).withdraw(any(Instant.class));
    verify(loginSessionStore).invalidateAll(userId);
  }

  @Test
  void rejectsDifferentProviderIdentityWithoutUnlinkingOrWithdrawal() {
    UUID userId = UUID.randomUUID();
    OAuthWithdrawalRequest request =
        new OAuthWithdrawalRequest(userId, Provider.GOOGLE, "original-sub");
    OAuthUserProfile otherAccount =
        new OAuthUserProfile(Provider.GOOGLE, "other-sub", "other@example.com", "other", null);

    assertThatThrownBy(() -> service.complete(request, otherAccount, "provider-access-token"))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OAUTH_ACCOUNT_MISMATCH));

    verifyNoInteractions(unlinkClient, loginSessionStore);
    verify(user, never()).withdraw(any());
  }

  @Test
  void rejectsPreparationForMissingUser() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.prepare(userId))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

    verifyNoInteractions(socialAccountRepository, requestStore);
  }

  @Test
  void rejectsPreparationForWithdrawnUser() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(user.getDeletedAt()).thenReturn(Instant.now());

    assertThatThrownBy(() -> service.prepare(userId))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_ACCOUNT_WITHDRAWN));

    verifyNoInteractions(socialAccountRepository, requestStore);
  }

  @Test
  void rejectsPreparationWithoutSocialAccount() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(socialAccountRepository.findByUserId(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.prepare(userId))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

    verifyNoInteractions(requestStore);
  }

  @Test
  void rejectsPreparationForLocalProvider() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(socialAccountRepository.findByUserId(userId)).thenReturn(Optional.of(socialAccount));
    when(socialAccount.getProvider()).thenReturn(Provider.LOCAL);

    assertThatThrownBy(() -> service.prepare(userId))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

    verifyNoInteractions(requestStore);
  }

  @Test
  void rejectsCompletionForMissingUser() {
    UUID userId = UUID.randomUUID();
    OAuthWithdrawalRequest request =
        new OAuthWithdrawalRequest(userId, Provider.GOOGLE, "google-sub");
    OAuthUserProfile profile =
        new OAuthUserProfile(Provider.GOOGLE, "google-sub", EMAIL, "user", null);
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.complete(request, profile, "provider-token"))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

    verifyNoInteractions(socialAccountRepository, unlinkClient, loginSessionStore);
  }

  @Test
  void rejectsCompletionForAlreadyWithdrawnUser() {
    UUID userId = UUID.randomUUID();
    OAuthWithdrawalRequest request =
        new OAuthWithdrawalRequest(userId, Provider.GOOGLE, "google-sub");
    OAuthUserProfile profile =
        new OAuthUserProfile(Provider.GOOGLE, "google-sub", EMAIL, "user", null);
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(user.getDeletedAt()).thenReturn(Instant.now());

    assertThatThrownBy(() -> service.complete(request, profile, "provider-token"))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_ACCOUNT_WITHDRAWN));

    verifyNoInteractions(socialAccountRepository, unlinkClient, loginSessionStore);
  }

  @Test
  void rejectsCompletionWhenStoredSocialAccountIsMissing() {
    UUID userId = UUID.randomUUID();
    OAuthWithdrawalRequest request =
        new OAuthWithdrawalRequest(userId, Provider.KAKAO, "kakao-sub");
    OAuthUserProfile profile =
        new OAuthUserProfile(Provider.KAKAO, "kakao-sub", EMAIL, "user", null);
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(socialAccountRepository.findByUserId(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.complete(request, profile, "provider-token"))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OAUTH_ACCOUNT_MISMATCH));

    verifyNoInteractions(unlinkClient, loginSessionStore);
    verify(user, never()).withdraw(any());
  }

  @Test
  void rejectsCompletionWhenStoredSocialAccountDoesNotMatch() {
    UUID userId = UUID.randomUUID();
    OAuthWithdrawalRequest request =
        new OAuthWithdrawalRequest(userId, Provider.GOOGLE, "google-sub");
    OAuthUserProfile profile =
        new OAuthUserProfile(Provider.GOOGLE, "google-sub", EMAIL, "user", null);
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(socialAccountRepository.findByUserId(userId)).thenReturn(Optional.of(socialAccount));
    when(socialAccount.getProvider()).thenReturn(Provider.GOOGLE);
    when(socialAccount.getProviderUserId()).thenReturn("other-sub");

    assertThatThrownBy(() -> service.complete(request, profile, "provider-token"))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OAUTH_ACCOUNT_MISMATCH));

    verifyNoInteractions(unlinkClient, loginSessionStore);
    verify(user, never()).withdraw(any());
  }

  @Test
  void doesNotWithdrawWhenProviderUnlinkFails() {
    UUID userId = UUID.randomUUID();
    OAuthWithdrawalRequest request =
        new OAuthWithdrawalRequest(userId, Provider.GOOGLE, "google-sub");
    OAuthUserProfile profile =
        new OAuthUserProfile(Provider.GOOGLE, "google-sub", EMAIL, "user", null);
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(socialAccountRepository.findByUserId(userId)).thenReturn(Optional.of(socialAccount));
    when(socialAccount.getProvider()).thenReturn(Provider.GOOGLE);
    when(socialAccount.getProviderUserId()).thenReturn("google-sub");
    doThrow(new IllegalStateException("unlink failed"))
        .when(unlinkClient).unlink(Provider.GOOGLE, "provider-token");

    assertThatThrownBy(() -> service.complete(request, profile, "provider-token"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("unlink failed");

    verify(user, never()).withdraw(any());
    verifyNoInteractions(loginSessionStore);
  }
}
