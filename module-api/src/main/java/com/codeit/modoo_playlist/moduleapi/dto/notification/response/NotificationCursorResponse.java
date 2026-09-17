package com.codeit.modoo_playlist.moduleapi.dto.notification.response;

import java.util.List;
import java.util.UUID;

public record NotificationCursorResponse(
        List<NotificationResponse> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        String sortDirection
) {
}
