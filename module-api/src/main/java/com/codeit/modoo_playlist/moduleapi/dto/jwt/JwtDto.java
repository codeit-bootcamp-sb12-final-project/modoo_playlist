package com.codeit.modoo_playlist.moduleapi.dto.jwt;

public record JwtDto(
        UserDto userDto,
        String accessToken
) {}
