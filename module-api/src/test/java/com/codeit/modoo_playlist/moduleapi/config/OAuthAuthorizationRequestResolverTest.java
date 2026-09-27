package com.codeit.modoo_playlist.moduleapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequestStore;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class OAuthAuthorizationRequestResolverTest {

  private final OAuthWithdrawalRequestStore withdrawalRequestStore =
      mock(OAuthWithdrawalRequestStore.class);
  private final OAuth2AuthorizationRequestResolver resolver =
      new OAuth2AuthorizationRequestConfig().oauth2AuthorizationRequestResolver(
          new InMemoryClientRegistrationRepository(
              registration("google"),
              registration("kakao")
          ),
          withdrawalRequestStore
      );

  @Test
  void addsSelectAccountPromptToGoogleAndKakaoRequests() {
    assertSelectAccountPrompt("/oauth2/authorization/google");
    assertSelectAccountPrompt("/oauth2/authorization/kakao");
  }

  @Test
  void bindsWithdrawalRequestToPrefixedOAuthState() {
    MockHttpServletRequest request = request("/oauth2/authorization/google");
    request.addParameter("withdrawalRequestId", "withdrawal-request-id");

    OAuth2AuthorizationRequest authorizationRequest = resolver.resolve(request);

    assertThat(authorizationRequest).isNotNull();
    assertThat(authorizationRequest.getState()).startsWith("withdrawal.");
    assertThat(authorizationRequest.getAuthorizationRequestUri())
        .contains("state=withdrawal.");
    verify(withdrawalRequestStore).bindState(
        "withdrawal-request-id",
        authorizationRequest.getState(),
        "google"
    );
  }

  private void assertSelectAccountPrompt(String requestUri) {
    OAuth2AuthorizationRequest authorizationRequest =
        resolver.resolve(request(requestUri));

    assertThat(authorizationRequest).isNotNull();
    assertThat(authorizationRequest.getAdditionalParameters())
        .containsEntry("prompt", "select_account");
    assertThat(authorizationRequest.getAuthorizationRequestUri())
        .contains("prompt=select_account");
  }

  private MockHttpServletRequest request(String requestUri) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(8080);
    return request;
  }

  private ClientRegistration registration(String registrationId) {
    return ClientRegistration.withRegistrationId(registrationId)
        .clientId(registrationId + "-client-id")
        .clientSecret(registrationId + "-client-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("openid", "email")
        .authorizationUri("https://accounts.example.test/oauth/authorize")
        .tokenUri("https://accounts.example.test/oauth/token")
        .jwkSetUri("https://accounts.example.test/.well-known/jwks.json")
        .userInfoUri("https://accounts.example.test/userinfo")
        .userNameAttributeName("sub")
        .clientName(registrationId)
        .build();
  }
}
