package com.codeit.modoo_playlist.moduleapi.security.oauth;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.AuthService;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.OAuthAccountService;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.OAuthWithdrawalService;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.SocialAccountUnlinkClient;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthAccountResult;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginIssueResult;
import com.codeit.modoo_playlist.moduleapi.security.LoginCredentialType;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequestStore;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class OAuthLoginSuccessHandlerTest {

  private static final String SUCCESS_REDIRECT_URI = "/#/contents";
  private static final String FAILURE_REDIRECT_URI = "/#/sign-in";

  @Mock
  JwtTokenProvider tokenProvider;
  @Mock
  AuthService authService;
  @Mock
  OAuthAccountService accountService;
  @Mock
  OAuthWithdrawalService withdrawalService;
  @Mock
  SocialAccountUnlinkClient socialAccountUnlinkClient;
  @Mock
  OAuthWithdrawalRequestStore withdrawalRequestStore;
  @Mock
  HttpServletRequest request;
  @Mock
  HttpServletResponse response;
  @Mock
  Authentication authentication;
  @Mock
  OidcUser oidcUser;

  private OAuthLoginSuccessHandler handler;

  @BeforeEach
  void setUp() {
    handler = new OAuthLoginSuccessHandler(
        authService,
        accountService,
        withdrawalService,
        socialAccountUnlinkClient,
        withdrawalRequestStore,
        tokenProvider,
        SUCCESS_REDIRECT_URI,
        FAILURE_REDIRECT_URI,
        "/#/sign-in",
        "/#/profiles/{userId}"
    );
  }

  @Test
  void storesRefreshTokenAndRedirectsToFrontend() throws Exception {
    UUID userId = UUID.randomUUID();
    Instant expiresAt = Instant.now().plusSeconds(3600);
    OAuthUserProfile profile = profile();
    OAuthUserPrincipal principal = new OAuthUserPrincipal(profile, "provider-token", oidcUser);
    UserDto user = new UserDto(
        userId,
        "user@example.com",
        "Google User",
        null,
        UserRole.USER,
        false,
        Instant.now()
    );
    LoginIssueResult result = new LoginIssueResult(
        user,
        "access-token",
        "refresh-token",
        expiresAt
    );
    Cookie refreshCookie = new Cookie("REFRESH_TOKEN", "refresh-token");

    when(authentication.getPrincipal()).thenReturn(principal);
    when(accountService.resolveOrCreate(profile))
        .thenReturn(new OAuthAccountResult(userId, false));
    when(authService.issueLogin(userId, LoginCredentialType.PERMANENT)).thenReturn(result);
    when(tokenProvider.generateRefreshTokenCookie("refresh-token", expiresAt))
        .thenReturn(refreshCookie);

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(authService).issueLogin(userId, LoginCredentialType.PERMANENT);
    verify(response).addCookie(refreshCookie);
    verify(response).sendRedirect(SUCCESS_REDIRECT_URI + "?oauthSuccess=true");
  }

  @Test
  void redirectsLockedOAuthAccountToSignIn() throws Exception {
    UUID userId = UUID.randomUUID();
    OAuthUserProfile profile = profile();
    OAuthUserPrincipal principal = new OAuthUserPrincipal(profile, "provider-token", oidcUser);

    when(authentication.getPrincipal()).thenReturn(principal);
    when(accountService.resolveOrCreate(profile))
        .thenReturn(new OAuthAccountResult(userId, false));
    when(authService.issueLogin(userId, LoginCredentialType.PERMANENT))
        .thenThrow(new BaseException(ErrorCode.USER_ACCOUNT_LOCKED));

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(response).sendRedirect(
        "/#/sign-in?oauthError=USER_ACCOUNT_LOCKED"
    );
    verify(response, never()).addCookie(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void completesWithdrawalWithoutIssuingServiceLogin() throws Exception {
    UUID userId = UUID.randomUUID();
    OAuthUserProfile profile = profile();
    OAuthUserPrincipal principal = new OAuthUserPrincipal(profile, "provider-token", oidcUser);
    OAuthWithdrawalRequest pending =
        new OAuthWithdrawalRequest(userId, Provider.GOOGLE, profile.providerUserId());
    Cookie expiredCookie = new Cookie(JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME, "");
    expiredCookie.setMaxAge(0);

    when(request.getParameter("state")).thenReturn("withdrawal.state");
    when(withdrawalRequestStore.isWithdrawalState("withdrawal.state")).thenReturn(true);
    when(withdrawalRequestStore.consumeByState("withdrawal.state"))
        .thenReturn(Optional.of(pending));
    when(authentication.getPrincipal()).thenReturn(principal);
    when(tokenProvider.generateRefreshTokenExpirationCookie()).thenReturn(expiredCookie);

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(withdrawalService).complete(pending, profile, "provider-token");
    verify(response).addCookie(expiredCookie);
    verify(response).sendRedirect("/#/sign-in?withdrawalSuccess=true");
    verifyNoInteractions(accountService, authService);
  }

  @Test
  void unlinksNewGrantWhenWithdrawnAccountAttemptsOAuthLogin() throws Exception {
    OAuthUserProfile profile = profile();
    OAuthUserPrincipal principal = new OAuthUserPrincipal(profile, "new-provider-token", oidcUser);
    when(authentication.getPrincipal()).thenReturn(principal);
    when(accountService.resolveOrCreate(profile))
        .thenThrow(new BaseException(ErrorCode.USER_ACCOUNT_WITHDRAWN));

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(socialAccountUnlinkClient).unlink(Provider.GOOGLE, "new-provider-token");
    verifyNoInteractions(authService);
    verify(response).sendRedirect(
        "/#/sign-in?oauthError=USER_ACCOUNT_WITHDRAWN"
    );
  }

  private OAuthUserProfile profile() {
    return new OAuthUserProfile(
        Provider.GOOGLE,
        "provider-user-id",
        "user@example.com",
        "Google User",
        null
    );
  }
}
