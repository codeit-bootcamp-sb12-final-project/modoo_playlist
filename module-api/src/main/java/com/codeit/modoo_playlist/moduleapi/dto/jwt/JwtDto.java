package com.codeit.modoo_playlist.moduleapi.dto.jwt;

import com.codeit.modoo_playlist.moduleapi.dto.UserDto;

public record JwtDto(
    UserDto userDto,
    String accessToken,
    LoginState loginState
) {

  public JwtDto(UserDto userDto, String accessToken) {
    this(userDto, accessToken, LoginState.AUTHENTICATED);
  }
}
