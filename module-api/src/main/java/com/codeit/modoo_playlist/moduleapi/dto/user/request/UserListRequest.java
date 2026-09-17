package com.codeit.modoo_playlist.moduleapi.dto.user.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UserListRequest(
    @Size(max = 255)
    String emailLike,

    @Pattern(
        regexp = "USER|ADMIN",
        message = "roleEqual은 USER 또는 ADMIN이어야 합니다."
    )
    String roleEqual,

    Boolean isLocked,

    String cursor,

    UUID idAfter,

    @NotNull
    @Min(1)
    @Max(100)
    Integer limit,

    @NotNull
    @Pattern(
        regexp = "ASCENDING|DESCENDING",
        message = "sortDirection은 ASCENDING 또는 DESCENDING이어야 합니다."
    )
    String sortDirection,

    @NotNull
    @Pattern(
        regexp = "name|email|createdAt|isLocked|role",
        message = "지원하지 않는 sortBy 값입니다."
    )
    String sortBy
) {

}
