package com.codeit.modoo_playlist.moduleapi.dto;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;

import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String email,
        String nickname,
        UserRole role
) {}
