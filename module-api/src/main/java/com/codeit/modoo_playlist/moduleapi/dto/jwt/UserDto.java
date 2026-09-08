package com.codeit.modoo_playlist.moduleapi.dto.jwt;

import com.codeit.blog.entity.UserRole;

import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String email,
        String nickname,
        UserRole role
) {}
