package com.codeit.modoo_playlist.moduleapi.domain.user.service;

import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.OAuthWithdrawalAuthorizationResponse;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequest;
import java.util.UUID;

public interface OAuthWithdrawalService {

  OAuthWithdrawalAuthorizationResponse prepare(UUID userId);

  void complete(
      OAuthWithdrawalRequest request,
      OAuthUserProfile authenticatedProfile,
      String providerAccessToken
  );
}
