package com.codeit.modoo_playlist.moduleapi.dto.user.request;

import jakarta.validation.constraints.NotBlank;

public record UserWithdrawalRequest(
    @NotBlank String password
) {
}
