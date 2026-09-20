package com.codeit.modoo_playlist.moduleapi.security;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
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

@ExtendWith(MockitoExtension.class)
class LoginFailureHandlerTest {

  @Mock
  SecurityErrorResponseWriter errorResponseWriter;

  @Mock
  HttpServletRequest request;

  @Mock
  HttpServletResponse response;

  private LoginFailureHandler handler;

  @BeforeEach
  void setUp() {
    handler = new LoginFailureHandler(errorResponseWriter, "/#/sign-in");
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
}