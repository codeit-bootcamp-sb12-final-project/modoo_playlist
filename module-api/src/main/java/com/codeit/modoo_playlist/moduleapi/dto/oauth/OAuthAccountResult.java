package com.codeit.modoo_playlist.moduleapi.dto.oauth;

import java.util.UUID;

public record OAuthAccountResult(
    UUID userId,
    boolean newlyRegistered
) {
}
