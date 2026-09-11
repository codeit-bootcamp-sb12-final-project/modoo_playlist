package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.moduleapi.dto.jwt.JwtInformation;

public interface JwtRegistry<T> {

  void registerJwtInformation(JwtInformation jwtInformation);

  void invalidateJwtInformationByUserId(T userId);

  boolean hasActiveJwtInformationByUserId(T userId);

  boolean hasActiveJwtInformationByAccessToken(String accessToken);

  boolean hasActiveJwtInformationByRefreshToken(String refreshToken);

  void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation);

  void clearExpiredJwtInformation();
}
