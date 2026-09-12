package com.codeit.modoo_playlist.moduleapi.dto.jwt;

import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import java.time.Instant;

public record TokenRefreshResult(
    UserDto userDto,
    String accessToken,
    String refreshToken,
    Instant expiresAt
) {

}
