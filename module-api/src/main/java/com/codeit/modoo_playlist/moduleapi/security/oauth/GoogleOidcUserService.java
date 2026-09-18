package com.codeit.modoo_playlist.moduleapi.security.oauth;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.OAuthAccountService;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthAccountResult;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import com.codeit.modoo_playlist.moduleapi.security.CodedAuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

  private static final String GOOGLE_REGISTRATION_ID = "google";
  private final OidcUserService delegate = new OidcUserService();

  private final OAuthAccountService accountService;
  private final GoogleOAuthUserProfileMapper profileMapper;

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) {
    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    if (!GOOGLE_REGISTRATION_ID.equals(registrationId)) {
      throw new CodedAuthenticationException(ErrorCode.INVALID_REQUEST);
    }

    OidcUser oidcUser = delegate.loadUser(userRequest);
    OAuthUserProfile profile = profileMapper.map(oidcUser.getClaims());

    try {
      OAuthAccountResult account = accountService.resolveOrCreate(profile);
      return new OAuthUserPrincipal(account.userId(), oidcUser);
    } catch (BaseException exception) {
      throw new CodedAuthenticationException(exception.getErrorCode());
    }
  }
}
