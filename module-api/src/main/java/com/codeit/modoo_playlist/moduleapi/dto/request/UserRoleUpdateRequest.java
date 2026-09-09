package com.codeit.modoo_playlist.moduleapi.dto.request;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import org.jetbrains.annotations.NotNull;

public record UserRoleUpdateRequest(
    @NotNull UserRole role
) {

}
