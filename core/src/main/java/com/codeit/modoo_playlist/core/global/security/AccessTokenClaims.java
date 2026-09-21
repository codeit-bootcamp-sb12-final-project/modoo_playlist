package com.codeit.modoo_playlist.core.global.security;

import java.util.UUID;

public record AccessTokenClaims(
        UUID userId,
        UUID sid,
        String email
) {

}