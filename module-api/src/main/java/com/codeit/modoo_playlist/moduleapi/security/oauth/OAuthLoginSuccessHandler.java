package com.codeit.modoo_playlist.moduleapi.security.oauth;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.AuthService;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginIssueResult;
import com.codeit.modoo_playlist.moduleapi.security.LoginCredentialType;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

  private final AuthService authService;
  private final JwtTokenProvider tokenProvider;
  private final String successRedirectUri;
  private final String failureRedirectUri;

  public OAuthLoginSuccessHandler(
      AuthService authService,
      JwtTokenProvider tokenProvider,
      @Value("${module-api.auth.oauth2.success-redirect-uri}") String successRedirectUri,
      @Value("${module-api.auth.oauth2.failure-redirect-uri}") String failureRedirectUri
  ) {
    this.authService = authService;
    this.tokenProvider = tokenProvider;
    this.successRedirectUri = successRedirectUri;
    this.failureRedirectUri = failureRedirectUri;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException {
    Object principal = authentication.getPrincipal();
    if (!(principal instanceof OAuthUserPrincipal oauthPrincipal)) {
      SecurityContextHolder.clearContext();
      log.error(
          "Unexpected principal type in OAuth login success handler: {}",
          principal == null ? "null" : principal.getClass().getName()
      );
      redirectFailure(response, ErrorCode.INTERNAL_SERVER_ERROR);
      return;
    }

    try {
      LoginIssueResult result = authService.issueLogin(
          oauthPrincipal.getUserId(),
          LoginCredentialType.PERMANENT
      );
      Cookie refreshCookie = tokenProvider.generateRefreshTokenCookie(
          result.refreshToken(),
          result.expiresAt()
      );

      response.addCookie(refreshCookie);
      response.sendRedirect(successRedirectUri);
    } catch (BaseException exception) {
      SecurityContextHolder.clearContext();
      log.warn("OAuth login processing failed: {}", exception.getErrorCode(), exception);
      redirectFailure(response, exception.getErrorCode());
    } catch (DataAccessException exception) {
      SecurityContextHolder.clearContext();
      log.error("Failed to store OAuth login session", exception);
      redirectFailure(response, ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE);
    } catch (Exception exception) {
      SecurityContextHolder.clearContext();
      log.error("Failed to complete OAuth login", exception);
      redirectFailure(response, ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private void redirectFailure(HttpServletResponse response, ErrorCode errorCode)
      throws IOException {
    String separator = failureRedirectUri.contains("?") ? "&" : "?";
    response.sendRedirect(
        failureRedirectUri + separator + "oauthError=" + errorCode.name()
    );
  }
}
