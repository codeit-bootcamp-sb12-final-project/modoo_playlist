package com.codeit.modoo_playlist.moduleapi.dto.follow.response;

import java.util.UUID;

public record FollowResponse(
        UUID id,
        UUID followeeId,
        UUID followerId
) {
}