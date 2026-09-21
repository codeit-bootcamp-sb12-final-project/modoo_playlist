package com.codeit.modoo_playlist.moduleapi.dto.follow.request;

import java.util.UUID;

public record FollowListRequest(
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection
) {

    public FollowListRequest {
        if (limit <= 0) {
            limit = 20;
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "DESCENDING";
        }
    }
}