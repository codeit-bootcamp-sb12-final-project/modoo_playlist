package com.codeit.modoo_playlist.moduleapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

@Configuration
public class OAuth2AuthorizationRequestConfig {

  //  로그인시 계정 선택 창으로 넘어가기 위한 파라미터 추가
  @Bean
  public OAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository
  ) {
    DefaultOAuth2AuthorizationRequestResolver resolver =
        new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository);

    resolver.setAuthorizationRequestCustomizer(request ->
        request.additionalParameters(parameters ->
            parameters.put("prompt", "select_account")));

    return resolver;
  }
}
