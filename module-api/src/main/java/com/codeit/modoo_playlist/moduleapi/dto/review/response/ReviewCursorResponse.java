package com.codeit.modoo_playlist.moduleapi.dto.review.response;

import java.util.List;
import java.util.UUID;

public record ReviewCursorResponse(
        List<ReviewResponse> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        String sortDirection
) {
}