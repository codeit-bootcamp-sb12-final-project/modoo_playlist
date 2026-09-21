package com.codeit.modoo_playlist.moduleapi.domain.follow.repository.query;

import java.util.UUID;

public record FollowListCondition(
        UUID userId,
        FollowListType type,
        String cursor,
        UUID idAfter,
        int limit,
        SortDirection sortDirection
) {

    public enum SortDirection {
        ASCENDING,
        DESCENDING
    }
}