package com.codeit.modoo_playlist.moduleapi.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import com.codeit.modoo_playlist.moduleapi.security.CodedAuthenticationException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class OAuthOidcUserServiceTest {

  @Mock
  OidcUserService delegate;
  @Mock
  GoogleOAuthUserProfileMapper googleProfileMapper;
  @Mock
  KakaoOAuthUserProfileMapper kakaoProfileMapper;
  @Mock
  OidcUserRequest userRequest;
  @Mock
  ClientRegistration clientRegistration;
  @Mock
  OidcUser oidcUser;
  @Mock
  OAuth2AccessToken accessToken;

  private OAuthOidcUserService service;

  @BeforeEach
  void setUp() {
    service = new OAuthOidcUserService(
        delegate,
        googleProfileMapper,
        kakaoProfileMapper
    );
  }

  @Test
  void logsInGoogleUser() {
    OAuthUserProfile profile = profile(Provider.GOOGLE);
    Map<String, Object> claims = Map.of("sub", "provider-user-id");
    prepareProvider("google");
    prepareAccessToken();

    when(oidcUser.getClaims()).thenReturn(claims);
    when(googleProfileMapper.map(claims)).thenReturn(profile);

    OidcUser result = service.loadUser(userRequest);

    assertThat(result).isInstanceOf(OAuthUserPrincipal.class);
    assertThat(((OAuthUserPrincipal) result).getOAuthProfile()).isEqualTo(profile);
    assertThat(((OAuthUserPrincipal) result).getProviderAccessToken())
        .isEqualTo("provider-access-token");
    verify(googleProfileMapper).map(claims);
    verify(kakaoProfileMapper, never()).map(claims);
  }

  @Test
  void logsInKakaoUser() {
    OAuthUserProfile profile = profile(Provider.KAKAO);
    Map<String, Object> claims = Map.of("sub", "provider-user-id");
    prepareProvider("kakao");
    prepareAccessToken();

    when(oidcUser.getClaims()).thenReturn(claims);
    when(kakaoProfileMapper.map(claims)).thenReturn(profile);

    OidcUser result = service.loadUser(userRequest);

    assertThat(result).isInstanceOf(OAuthUserPrincipal.class);
    assertThat(((OAuthUserPrincipal) result).getOAuthProfile()).isEqualTo(profile);
    verify(kakaoProfileMapper).map(claims);
    verify(googleProfileMapper, never()).map(claims);
  }

  @Test
  void rejectsUnsupportedProvider() {
    prepareProvider("unsupported");

    assertThatThrownBy(() -> service.loadUser(userRequest))
        .isInstanceOfSatisfying(CodedAuthenticationException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
        );

    verifyNoInteractions(googleProfileMapper, kakaoProfileMapper);
  }

  @Test
  void propagatesProviderAuthenticationFailure() {
    OAuth2AuthenticationException providerFailure =
        new OAuth2AuthenticationException(new OAuth2Error("invalid_id_token"));
    when(delegate.loadUser(userRequest)).thenThrow(providerFailure);

    assertThatThrownBy(() -> service.loadUser(userRequest))
        .isSameAs(providerFailure);

    verifyNoInteractions(googleProfileMapper, kakaoProfileMapper);
  }

  private void prepareProvider(String registrationId) {
    when(delegate.loadUser(userRequest)).thenReturn(oidcUser);
    when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
    when(clientRegistration.getRegistrationId()).thenReturn(registrationId);
  }

  private void prepareAccessToken() {
    when(userRequest.getAccessToken()).thenReturn(accessToken);
    when(accessToken.getTokenValue()).thenReturn("provider-access-token");
  }

  private OAuthUserProfile profile(Provider provider) {
    return new OAuthUserProfile(
        provider,
        "provider-user-id",
        "user@example.com",
        "OAuth User",
        null
    );
  }
}
