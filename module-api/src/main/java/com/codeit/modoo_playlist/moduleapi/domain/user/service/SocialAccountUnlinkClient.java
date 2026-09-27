package com.codeit.modoo_playlist.moduleapi.domain.user.service;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;

public interface SocialAccountUnlinkClient {

  void unlink(Provider provider, String accessToken);
}
