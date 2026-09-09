package com.codeit.modoo_playlist.moduleapi.dto.content.response;

import java.util.List;
import java.util.UUID;

public record ContentCursorResponse(
        List<ContentListItemResponse> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        String sortDirection
) {
}
