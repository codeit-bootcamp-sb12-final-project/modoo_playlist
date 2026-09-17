package com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query;

import java.util.UUID;

public record NotificationListCondition(
        UUID receiverId,
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
