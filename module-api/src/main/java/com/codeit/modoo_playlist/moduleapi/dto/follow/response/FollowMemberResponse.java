package com.codeit.modoo_playlist.moduleapi.dto.follow.response;

import com.codeit.modoo_playlist.core.domain.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record FollowMemberResponse(
        UUID followId,
        UserSummaryResponse user,
        Instant followedAt
) {

}