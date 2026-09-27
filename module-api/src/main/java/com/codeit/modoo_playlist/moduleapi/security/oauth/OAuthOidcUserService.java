package com.codeit.modoo_playlist.moduleapi.security.oauth;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import com.codeit.modoo_playlist.moduleapi.security.CodedAuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class OAuthOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

  private static final String GOOGLE_REGISTRATION_ID = "google";
  private static final String KAKAO_REGISTRATION_ID = "kakao";

  private final OidcUserService delegate;
  private final GoogleOAuthUserProfileMapper googleProfileMapper;
  private final KakaoOAuthUserProfileMapper kakaoProfileMapper;

  @Autowired
  public OAuthOidcUserService(
      GoogleOAuthUserProfileMapper googleProfileMapper,
      KakaoOAuthUserProfileMapper kakaoProfileMapper
  ) {
    this(
        new OidcUserService(),
        googleProfileMapper,
        kakaoProfileMapper
    );
  }

  OAuthOidcUserService(
      OidcUserService delegate,
      GoogleOAuthUserProfileMapper googleProfileMapper,
      KakaoOAuthUserProfileMapper kakaoProfileMapper
  ) {
    this.delegate = delegate;
    this.googleProfileMapper = googleProfileMapper;
    this.kakaoProfileMapper = kakaoProfileMapper;
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) {
    OidcUser oidcUser = delegate.loadUser(userRequest);
    String registrationId = userRequest.getClientRegistration().getRegistrationId();

    OAuthUserProfile profile = switch (registrationId) {
      case GOOGLE_REGISTRATION_ID -> googleProfileMapper.map(oidcUser.getClaims());
      case KAKAO_REGISTRATION_ID -> kakaoProfileMapper.map(oidcUser.getClaims());
      default -> throw new CodedAuthenticationException(ErrorCode.INVALID_REQUEST);
    };

    return new OAuthUserPrincipal(
        profile,
        userRequest.getAccessToken().getTokenValue(),
        oidcUser
    );
  }
}
