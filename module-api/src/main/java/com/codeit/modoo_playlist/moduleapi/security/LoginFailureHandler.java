package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

  private static final String OAUTH_CALLBACK_BASE_URI = "/login/oauth2/code/";

  private final SecurityErrorResponseWriter errorResponseWriter;
  private final String oauthFailureRedirectUri;

  public LoginFailureHandler(
      SecurityErrorResponseWriter errorResponseWriter,
      @Value("${module-api.auth.oauth2.failure-redirect-uri}") String oauthFailureRedirectUri
  ) {
    this.errorResponseWriter = errorResponseWriter;
    this.oauthFailureRedirectUri = oauthFailureRedirectUri;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception
  ) throws IOException {
    ErrorCode errorCode = resolveErrorCode(request, exception);

    if (request.getRequestURI().startsWith(OAUTH_CALLBACK_BASE_URI)) {
      redirectOAuthFailure(response, errorCode);
      return;
    }

    errorResponseWriter.write(response, errorCode);
  }

  private ErrorCode resolveErrorCode(
      HttpServletRequest request,
      AuthenticationException exception
  ) {
    CodedAuthenticationException codedException = findCause(
        exception,
        CodedAuthenticationException.class
    );

    if (codedException != null) {
      return codedException.getErrorCode();
    }
    if (findCause(exception, LockedException.class) != null) {
      return ErrorCode.USER_ACCOUNT_LOCKED;
    }
    if (isInvalidCredentials(exception)) {
      return ErrorCode.INVALID_CREDENTIALS;
    }
    if (exception instanceof OAuth2AuthenticationException) {
      return ErrorCode.INVALID_CREDENTIALS;
    }
    if (findCause(exception, DataAccessException.class) != null) {
      return ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE;
    }

    log.error(
        "Unexpected authentication failure at {}: {}",
        request.getRequestURI(),
        exception.getClass().getName(),
        exception
    );
    return ErrorCode.INTERNAL_SERVER_ERROR;
  }

  private void redirectOAuthFailure(HttpServletResponse response, ErrorCode errorCode)
      throws IOException {
    String separator = oauthFailureRedirectUri.contains("?") ? "&" : "?";
    response.sendRedirect(
        oauthFailureRedirectUri + separator + "oauthError=" + errorCode.name()
    );
  }

  private boolean isInvalidCredentials(AuthenticationException exception) {
    return findCause(exception, BadCredentialsException.class) != null
        || findCause(exception, UsernameNotFoundException.class) != null;
  }

  private <T extends Throwable> T findCause(Throwable exception, Class<T> causeType) {
    Throwable current = exception;
    while (current != null) {
      if (causeType.isInstance(current)) {
        return causeType.cast(current);
      }
      current = current.getCause();
    }
    return null;
  }
}
