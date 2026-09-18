package com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query;

import com.codeit.modoo_playlist.core.domain.notification.entity.Notification;
import java.util.List;
import java.util.UUID;

public record NotificationQueryPage(
        List<Notification> notifications,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount
) {

    public NotificationQueryPage {
        notifications = List.copyOf(notifications);
    }
}
