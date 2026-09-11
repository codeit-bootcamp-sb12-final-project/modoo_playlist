package com.codeit.modoo_playlist.moduleapi.dto.playlist.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PlaylistListRequest(
        @Size(max = 255)
        String keywordLike,

        UUID ownerIdEqual,

        UUID subscriberIdEqual,

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
                regexp = "updatedAt|createdAt",
                message = "지원하지 않는 sortBy 값입니다."
        )
        String sortBy
) {
}