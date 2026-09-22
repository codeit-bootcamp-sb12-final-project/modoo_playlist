package com.codeit.modoo_playlist.moduleapi.security.oauth;

import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class OAuthUserPrincipal implements OidcUser {

  private final OAuthUserProfile profile;
  private final String providerAccessToken;
  private final OidcUser delegate;

  public OAuthUserPrincipal(
      OAuthUserProfile profile,
      String providerAccessToken,
      OidcUser delegate
  ) {
    this.profile = profile;
    this.providerAccessToken = providerAccessToken;
    this.delegate = delegate;
  }

  public OAuthUserProfile getOAuthProfile() {
    return profile;
  }

  public String getProviderAccessToken() {
    return providerAccessToken;
  }

  @Override
  public Map<String, Object> getClaims() {
    return delegate.getClaims();
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return delegate.getUserInfo();
  }

  @Override
  public OidcIdToken getIdToken() {
    return delegate.getIdToken();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return delegate.getAttributes();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return delegate.getAuthorities();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }
}
