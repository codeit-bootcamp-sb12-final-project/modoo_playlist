package com.codeit.modoo_playlist.moduleapi.security.oauth;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import com.codeit.modoo_playlist.moduleapi.security.CodedAuthenticationException;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthUserProfileMapper {

  public OAuthUserProfile map(Map<String, Object> claims) {
    String providerUserId = requiredString(claims, "sub");
    String email = requiredString(claims, "email").toLowerCase(Locale.ROOT);

    if (!Boolean.TRUE.equals(claims.get("email_verified"))) {
      throw new CodedAuthenticationException(ErrorCode.INVALID_CREDENTIALS);
    }

    String name = optionalString(claims, "name");
    if (name == null) {
      int atIndex = email.indexOf('@');
      name = atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    return new OAuthUserProfile(
        Provider.GOOGLE,
        providerUserId,
        email,
        name,
        optionalString(claims, "picture")
    );
  }

  private String requiredString(Map<String, Object> claims, String key) {
    String value = optionalString(claims, key);
    if (value == null) {
      throw new CodedAuthenticationException(ErrorCode.INVALID_CREDENTIALS);
    }
    return value;
  }

  private String optionalString(Map<String, Object> claims, String key) {
    Object value = claims.get(key);
    if (!(value instanceof String stringValue) || stringValue.isBlank()) {
      return null;
    }
    return stringValue.trim();
  }
}
