package com.codeit.modoo_playlist.moduleapi.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.OAuthAccountService;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthAccountResult;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import com.codeit.modoo_playlist.moduleapi.security.CodedAuthenticationException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class OAuthOidcUserServiceTest {

  @Mock
  OidcUserService delegate;
  @Mock
  OAuthAccountService accountService;
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

  private OAuthOidcUserService service;

  @BeforeEach
  void setUp() {
    service = new OAuthOidcUserService(
        delegate,
        accountService,
        googleProfileMapper,
        kakaoProfileMapper
    );
  }

  @Test
  void logsInGoogleUser() {
    OAuthUserProfile profile = profile(Provider.GOOGLE);
    UUID userId = UUID.randomUUID();
    Map<String, Object> claims = Map.of("sub", "provider-user-id");
    prepareProvider("google");

    when(oidcUser.getClaims()).thenReturn(claims);
    when(googleProfileMapper.map(claims)).thenReturn(profile);
    when(accountService.resolveOrCreate(profile))
        .thenReturn(new OAuthAccountResult(userId, false));

    OidcUser result = service.loadUser(userRequest);

    assertThat(result).isInstanceOf(OAuthUserPrincipal.class);
    assertThat(((OAuthUserPrincipal) result).getUserId()).isEqualTo(userId);
    verify(googleProfileMapper).map(claims);
    verify(kakaoProfileMapper, never()).map(claims);
  }

  @Test
  void logsInKakaoUser() {
    OAuthUserProfile profile = profile(Provider.KAKAO);
    UUID userId = UUID.randomUUID();
    Map<String, Object> claims = Map.of("sub", "provider-user-id");
    prepareProvider("kakao");

    when(oidcUser.getClaims()).thenReturn(claims);
    when(kakaoProfileMapper.map(claims)).thenReturn(profile);
    when(accountService.resolveOrCreate(profile))
        .thenReturn(new OAuthAccountResult(userId, true));

    OidcUser result = service.loadUser(userRequest);

    assertThat(result).isInstanceOf(OAuthUserPrincipal.class);
    assertThat(((OAuthUserPrincipal) result).getUserId()).isEqualTo(userId);
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

    verifyNoInteractions(googleProfileMapper, kakaoProfileMapper, accountService);
  }

  @Test
  void convertsAccountFailureToAuthenticationFailure() {
    OAuthUserProfile profile = profile(Provider.GOOGLE);
    Map<String, Object> claims = Map.of("sub", "provider-user-id");
    prepareProvider("google");

    when(oidcUser.getClaims()).thenReturn(claims);
    when(googleProfileMapper.map(claims)).thenReturn(profile);
    when(accountService.resolveOrCreate(profile))
        .thenThrow(new BaseException(ErrorCode.USER_ACCOUNT_LOCKED));

    assertThatThrownBy(() -> service.loadUser(userRequest))
        .isInstanceOfSatisfying(CodedAuthenticationException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_ACCOUNT_LOCKED)
        );
  }

  @Test
  void rejectsOAuthSignupWhenEmailAlreadyExists() {
    OAuthUserProfile profile = profile(Provider.GOOGLE);
    Map<String, Object> claims = Map.of("sub", "new-provider-user-id");
    prepareProvider("google");

    when(oidcUser.getClaims()).thenReturn(claims);
    when(googleProfileMapper.map(claims)).thenReturn(profile);
    when(accountService.resolveOrCreate(profile))
        .thenThrow(new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS));

    assertThatThrownBy(() -> service.loadUser(userRequest))
        .isInstanceOfSatisfying(CodedAuthenticationException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS)
        );
  }

  @Test
  void propagatesProviderAuthenticationFailure() {
    OAuth2AuthenticationException providerFailure =
        new OAuth2AuthenticationException(new OAuth2Error("invalid_id_token"));
    when(delegate.loadUser(userRequest)).thenThrow(providerFailure);

    assertThatThrownBy(() -> service.loadUser(userRequest))
        .isSameAs(providerFailure);

    verifyNoInteractions(googleProfileMapper, kakaoProfileMapper, accountService);
  }

  private void prepareProvider(String registrationId) {
    when(delegate.loadUser(userRequest)).thenReturn(oidcUser);
    when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
    when(clientRegistration.getRegistrationId()).thenReturn(registrationId);
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
