package com.codeit.modoo_playlist.moduleapi.config;

import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequestStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Configuration
public class OAuth2AuthorizationRequestConfig {

  //  로그인시 계정 선택 창으로 넘어가기 위한 파라미터 추가
  @Bean
  public OAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository,
      OAuthWithdrawalRequestStore withdrawalRequestStore
  ) {
    DefaultOAuth2AuthorizationRequestResolver resolver =
        new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository);

    resolver.setAuthorizationRequestCustomizer(request ->
        request.additionalParameters(parameters ->
            parameters.put("prompt", "select_account")));

    return new OAuth2AuthorizationRequestResolver() {
      @Override
      public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customizeWithdrawalRequest(request, resolver.resolve(request), null);
      }

      @Override
      public OAuth2AuthorizationRequest resolve(
          HttpServletRequest request,
          String clientRegistrationId
      ) {
        return customizeWithdrawalRequest(
            request,
            resolver.resolve(request, clientRegistrationId),
            clientRegistrationId
        );
      }

      private OAuth2AuthorizationRequest customizeWithdrawalRequest(
          HttpServletRequest request,
          OAuth2AuthorizationRequest authorizationRequest,
          String registrationId
      ) {
        String withdrawalRequestId = request.getParameter("withdrawalRequestId");
        if (authorizationRequest == null
            || withdrawalRequestId == null
            || withdrawalRequestId.isBlank()) {
          return authorizationRequest;
        }

        String resolvedRegistrationId = registrationId != null
            ? registrationId
            : request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
        String withdrawalState = OAuthWithdrawalRequestStore.WITHDRAWAL_STATE_PREFIX
            + authorizationRequest.getState();
        withdrawalRequestStore.bindState(
            withdrawalRequestId,
            withdrawalState,
            resolvedRegistrationId
        );

        return OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri(authorizationRequest.getAuthorizationUri())
            .clientId(authorizationRequest.getClientId())
            .redirectUri(authorizationRequest.getRedirectUri())
            .scopes(authorizationRequest.getScopes())
            .state(withdrawalState)
            .additionalParameters(authorizationRequest.getAdditionalParameters())
            .attributes(authorizationRequest.getAttributes())
            .build();
      }
    };
  }
}
