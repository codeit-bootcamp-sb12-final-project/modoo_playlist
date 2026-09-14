package com.codeit.modoo_playlist.moduleapi.domain.user.service;

import com.codeit.modoo_playlist.moduleapi.dto.jwt.TokenRefreshResult;

public interface AuthService {

  TokenRefreshResult refresh(String refreshToken);

}
