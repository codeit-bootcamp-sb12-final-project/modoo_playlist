package com.codeit.modoo_playlist.moduleapi.security;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequestStore;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequest;
import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class LoginFailureHandlerTest {

  @Mock
  SecurityErrorResponseWriter errorResponseWriter;

  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  @Mock
  OAuthWithdrawalRequestStore withdrawalRequestStore;

  private LoginFailureHandler handler;

  @BeforeEach
  void setUp() {
    handler = new LoginFailureHandler(
        errorResponseWriter,
        withdrawalRequestStore,
        "/#/sign-in",
        "/#/profiles/{userId}"
    );
  }

  @Test
  void redirectsOAuthFailureWhenContextPathIsConfigured() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/login/oauth2/code/google");
    when(request.getContextPath()).thenReturn("/api");

    handler.onAuthenticationFailure(
        request,
        response,
        new OAuth2AuthenticationException(new OAuth2Error("access_denied"))
    );

    verify(response).sendRedirect("/#/sign-in?oauthError=INVALID_CREDENTIALS");
    verify(errorResponseWriter, never()).write(response, ErrorCode.INVALID_CREDENTIALS);
  }

  @Test
  void writesJsonForFormLoginFailureWhenContextPathIsConfigured() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/api/auth/sign-in");
    when(request.getContextPath()).thenReturn("/api");

    handler.onAuthenticationFailure(
        request,
        response,
        new BadCredentialsException("invalid credentials")
    );

    verify(errorResponseWriter).write(response, ErrorCode.INVALID_CREDENTIALS);
    verify(response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void redirectsCancelledWithdrawalBackToProfile() throws Exception {
    UUID userId = UUID.randomUUID();
    OAuthWithdrawalRequest pending =
        new OAuthWithdrawalRequest(userId, Provider.GOOGLE, "google-sub");
    when(request.getRequestURI()).thenReturn("/login/oauth2/code/google");
    when(request.getContextPath()).thenReturn("");
    when(request.getParameter("state")).thenReturn("withdrawal.state");
    when(withdrawalRequestStore.isWithdrawalState("withdrawal.state")).thenReturn(true);
    when(withdrawalRequestStore.consumeByState("withdrawal.state"))
        .thenReturn(Optional.of(pending));

    handler.onAuthenticationFailure(
        request,
        response,
        new OAuth2AuthenticationException(new OAuth2Error("access_denied"))
    );

    verify(response).sendRedirect(
        "/#/profiles/" + userId
            + "?withdrawalModal=true&withdrawalError=OAUTH_REAUTHENTICATION_FAILED"
    );
  }
}