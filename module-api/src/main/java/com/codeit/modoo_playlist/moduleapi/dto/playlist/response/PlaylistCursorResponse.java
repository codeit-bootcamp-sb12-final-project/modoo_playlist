package com.codeit.modoo_playlist.moduleapi.dto.playlist.response;

import java.util.List;
import java.util.UUID;

public record PlaylistCursorResponse(
        List<PlaylistResponse> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        String sortDirection
) {
}