package com.codeit.modoo_playlist.moduleapi.dto.user.request;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(
    @NotNull UserRole role
) {

}
