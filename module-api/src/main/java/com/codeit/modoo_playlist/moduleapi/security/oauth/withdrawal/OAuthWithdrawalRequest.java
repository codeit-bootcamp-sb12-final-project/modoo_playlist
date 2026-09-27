package com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import java.util.UUID;

public record OAuthWithdrawalRequest(
    UUID userId,
    Provider provider,
    String providerUserId
) {
}
