package com.codeit.modoo_playlist.moduleapi.security.oauth;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.AuthService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginIssueResult;
import com.codeit.modoo_playlist.moduleapi.security.LoginCredentialType;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.UUID;
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
        tokenProvider,
        SUCCESS_REDIRECT_URI,
        FAILURE_REDIRECT_URI
    );
  }

  @Test
  void storesRefreshTokenAndRedirectsToFrontend() throws Exception {
    UUID userId = UUID.randomUUID();
    Instant expiresAt = Instant.now().plusSeconds(3600);
    OAuthUserPrincipal principal = new OAuthUserPrincipal(userId, oidcUser);
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
    when(authService.issueLogin(userId, LoginCredentialType.PERMANENT)).thenReturn(result);
    when(tokenProvider.generateRefreshTokenCookie("refresh-token", expiresAt))
        .thenReturn(refreshCookie);

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(authService).issueLogin(userId, LoginCredentialType.PERMANENT);
    verify(response).addCookie(refreshCookie);
    verify(response).sendRedirect(SUCCESS_REDIRECT_URI);
  }

  @Test
  void redirectsLockedOAuthAccountToSignIn() throws Exception {
    UUID userId = UUID.randomUUID();
    OAuthUserPrincipal principal = new OAuthUserPrincipal(userId, oidcUser);

    when(authentication.getPrincipal()).thenReturn(principal);
    when(authService.issueLogin(userId, LoginCredentialType.PERMANENT))
        .thenThrow(new BaseException(ErrorCode.USER_ACCOUNT_LOCKED));

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(response).sendRedirect(
        "/#/sign-in?oauthError=USER_ACCOUNT_LOCKED"
    );
    verify(response, never()).addCookie(org.mockito.ArgumentMatchers.any());
  }
}
