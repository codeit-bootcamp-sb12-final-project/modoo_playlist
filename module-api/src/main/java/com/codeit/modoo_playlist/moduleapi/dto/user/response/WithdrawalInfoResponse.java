package com.codeit.modoo_playlist.moduleapi.dto.user.response;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;

public record WithdrawalInfoResponse(
    WithdrawalVerificationMethod verificationMethod,
    Provider provider
) {
}
