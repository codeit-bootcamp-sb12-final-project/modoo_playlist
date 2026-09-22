package com.codeit.modoo_playlist.moduleapi.domain.follow.repository.query;

import com.codeit.modoo_playlist.core.domain.follow.entity.Follow;
import java.util.List;
import java.util.UUID;

public record FollowQueryPage(
        List<Follow> follows,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount
) {

    public FollowQueryPage {
        follows = List.copyOf(follows);
    }
}