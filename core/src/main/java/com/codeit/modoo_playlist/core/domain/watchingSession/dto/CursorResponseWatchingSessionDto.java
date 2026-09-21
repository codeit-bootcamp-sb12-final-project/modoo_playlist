package com.codeit.modoo_playlist.core.domain.watchingSession.dto;

import com.codeit.modoo_playlist.core.domain.conversation.entity.SortDirection;

import java.util.List;
import java.util.UUID;

public record CursorResponseWatchingSessionDto(
        List<WatchingSessionDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        SortDirection sortDirection
) {
}
