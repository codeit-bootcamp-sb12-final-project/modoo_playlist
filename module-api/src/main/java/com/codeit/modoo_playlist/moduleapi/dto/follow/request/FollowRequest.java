package com.codeit.modoo_playlist.moduleapi.dto.follow.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FollowRequest(
        @NotNull UUID followeeId
) {
}