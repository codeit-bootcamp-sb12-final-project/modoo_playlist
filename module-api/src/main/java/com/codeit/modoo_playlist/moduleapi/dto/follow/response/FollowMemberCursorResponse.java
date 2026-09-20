package com.codeit.modoo_playlist.moduleapi.dto.follow.response;

import java.util.List;
import java.util.UUID;

public record FollowMemberCursorResponse(
        List<FollowMemberResponse> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        String sortDirection
) {

}