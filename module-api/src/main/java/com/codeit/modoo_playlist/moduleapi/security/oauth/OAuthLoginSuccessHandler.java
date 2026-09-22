package com.codeit.modoo_playlist.moduleapi.security.oauth;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.AuthService;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.OAuthAccountService;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.OAuthWithdrawalService;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.SocialAccountUnlinkClient;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthAccountResult;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginIssueResult;
import com.codeit.modoo_playlist.moduleapi.security.LoginCredentialType;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequest;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequestStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
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
  private final OAuthAccountService accountService;
  private final OAuthWithdrawalService withdrawalService;
  private final SocialAccountUnlinkClient socialAccountUnlinkClient;
  private final OAuthWithdrawalRequestStore withdrawalRequestStore;
  private final JwtTokenProvider tokenProvider;
  private final String successRedirectUri;
  private final String failureRedirectUri;
  private final String withdrawalSuccessRedirectUri;
  private final String withdrawalFailureRedirectUri;

  public OAuthLoginSuccessHandler(
      AuthService authService,
      OAuthAccountService accountService,
      OAuthWithdrawalService withdrawalService,
      SocialAccountUnlinkClient socialAccountUnlinkClient,
      OAuthWithdrawalRequestStore withdrawalRequestStore,
      JwtTokenProvider tokenProvider,
      @Value("${module-api.auth.oauth2.success-redirect-uri}") String successRedirectUri,
      @Value("${module-api.auth.oauth2.failure-redirect-uri}") String failureRedirectUri,
      @Value("${module-api.auth.oauth2.withdrawal.success-redirect-uri}")
      String withdrawalSuccessRedirectUri,
      @Value("${module-api.auth.oauth2.withdrawal.failure-redirect-uri}")
      String withdrawalFailureRedirectUri
  ) {
    this.authService = authService;
    this.accountService = accountService;
    this.withdrawalService = withdrawalService;
    this.socialAccountUnlinkClient = socialAccountUnlinkClient;
    this.withdrawalRequestStore = withdrawalRequestStore;
    this.tokenProvider = tokenProvider;
    this.successRedirectUri = successRedirectUri;
    this.failureRedirectUri = failureRedirectUri;
    this.withdrawalSuccessRedirectUri = withdrawalSuccessRedirectUri;
    this.withdrawalFailureRedirectUri = withdrawalFailureRedirectUri;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException {
    String state = request.getParameter("state");
    boolean withdrawalFlow = withdrawalRequestStore.isWithdrawalState(state);
    Optional<OAuthWithdrawalRequest> withdrawalRequest = withdrawalFlow
        ? withdrawalRequestStore.consumeByState(state)
        : Optional.empty();

    if (withdrawalFlow && withdrawalRequest.isEmpty()) {
      SecurityContextHolder.clearContext();
      redirectWithdrawalFailure(response, null, ErrorCode.WITHDRAWAL_REQUEST_EXPIRED);
      return;
    }

    Object principal = authentication.getPrincipal();
    if (!(principal instanceof OAuthUserPrincipal oauthPrincipal)) {
      SecurityContextHolder.clearContext();
      log.error(
          "Unexpected principal type in OAuth login success handler: {}",
          principal == null ? "null" : principal.getClass().getName()
      );
      if (withdrawalFlow) {
        redirectWithdrawalFailure(
            response,
            withdrawalRequest.map(OAuthWithdrawalRequest::userId).orElse(null),
            ErrorCode.OAUTH_REAUTHENTICATION_FAILED
        );
      } else {
        redirectFailure(response, ErrorCode.INTERNAL_SERVER_ERROR);
      }
      return;
    }

    try {
      if (withdrawalRequest.isPresent()) {
        OAuthWithdrawalRequest pendingRequest = withdrawalRequest.get();
        withdrawalService.complete(
            pendingRequest,
            oauthPrincipal.getOAuthProfile(),
            oauthPrincipal.getProviderAccessToken()
        );
        response.addCookie(tokenProvider.generateRefreshTokenExpirationCookie());
        SecurityContextHolder.clearContext();
        redirectWithdrawalSuccess(response);
        return;
      }

      OAuthAccountResult account = accountService.resolveOrCreate(oauthPrincipal.getOAuthProfile());
      LoginIssueResult result = authService.issueLogin(
          account.userId(),
          LoginCredentialType.PERMANENT
      );
      Cookie refreshCookie = tokenProvider.generateRefreshTokenCookie(
          result.refreshToken(),
          result.expiresAt()
      );

      response.addCookie(refreshCookie);
      redirectSuccess(response);
    } catch (BaseException exception) {
      SecurityContextHolder.clearContext();
      log.warn("OAuth login processing failed: {}", exception.getErrorCode(), exception);
      if (withdrawalFlow) {
        redirectWithdrawalFailure(
            response,
            withdrawalRequest.map(OAuthWithdrawalRequest::userId).orElse(null),
            exception.getErrorCode()
        );
      } else {
        unlinkReauthorizedWithdrawnAccount(oauthPrincipal, exception);
        redirectFailure(response, exception.getErrorCode());
      }
    } catch (DataAccessException exception) {
      SecurityContextHolder.clearContext();
      log.error("Failed to store OAuth login session", exception);
      if (withdrawalFlow) {
        redirectWithdrawalFailure(
            response,
            withdrawalRequest.map(OAuthWithdrawalRequest::userId).orElse(null),
            ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE
        );
      } else {
        redirectFailure(response, ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE);
      }
    } catch (Exception exception) {
      SecurityContextHolder.clearContext();
      log.error("Failed to complete OAuth login", exception);
      if (withdrawalFlow) {
        redirectWithdrawalFailure(
            response,
            withdrawalRequest.map(OAuthWithdrawalRequest::userId).orElse(null),
            ErrorCode.INTERNAL_SERVER_ERROR
        );
      } else {
        redirectFailure(response, ErrorCode.INTERNAL_SERVER_ERROR);
      }
    }
  }

  private void redirectSuccess(HttpServletResponse response) throws IOException {
    String separator = successRedirectUri.contains("?") ? "&" : "?";
    response.sendRedirect(
        successRedirectUri + separator + "oauthSuccess=true"
    );
  }

  private void redirectFailure(HttpServletResponse response, ErrorCode errorCode)
      throws IOException {
    String separator = failureRedirectUri.contains("?") ? "&" : "?";
    response.sendRedirect(
        failureRedirectUri + separator + "oauthError=" + errorCode.name()
    );
  }

  private void redirectWithdrawalSuccess(HttpServletResponse response) throws IOException {
    String separator = withdrawalSuccessRedirectUri.contains("?") ? "&" : "?";
    response.sendRedirect(
        withdrawalSuccessRedirectUri + separator + "withdrawalSuccess=true"
    );
  }

  private void unlinkReauthorizedWithdrawnAccount(
      OAuthUserPrincipal oauthPrincipal,
      BaseException authenticationFailure
  ) {
    if (authenticationFailure.getErrorCode() != ErrorCode.USER_ACCOUNT_WITHDRAWN) {
      return;
    }

    try {
      socialAccountUnlinkClient.unlink(
          oauthPrincipal.getOAuthProfile().provider(),
          oauthPrincipal.getProviderAccessToken()
      );
    } catch (BaseException unlinkFailure) {
      log.error(
          "Failed to unlink reauthorized OAuth grant for withdrawn account: {}",
          oauthPrincipal.getOAuthProfile().provider(),
          unlinkFailure
      );
    }
  }

  private void redirectWithdrawalFailure(
      HttpServletResponse response,
      UUID userId,
      ErrorCode errorCode
  ) throws IOException {
    String redirectUri = userId == null
        ? failureRedirectUri
        : withdrawalFailureRedirectUri.replace("{userId}", userId.toString());
    String separator = redirectUri.contains("?") ? "&" : "?";
    response.sendRedirect(
        redirectUri + separator + "withdrawalModal=true&withdrawalError=" + errorCode.name()
    );
  }
}
