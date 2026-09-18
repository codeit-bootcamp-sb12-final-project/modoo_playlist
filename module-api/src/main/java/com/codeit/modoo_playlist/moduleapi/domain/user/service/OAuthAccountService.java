package com.codeit.modoo_playlist.moduleapi.domain.user.service;

import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthAccountResult;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;

public interface OAuthAccountService {

  OAuthAccountResult resolveOrCreate(OAuthUserProfile profile);
}
