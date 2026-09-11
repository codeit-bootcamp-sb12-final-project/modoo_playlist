package com.codeit.modoo_playlist.moduleapi.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserLockUpdateRequest(
    @NotNull Boolean locked
) {

}
