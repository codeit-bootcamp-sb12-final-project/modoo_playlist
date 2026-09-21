package com.codeit.modoo_playlist.moduleapi.dto.notification.response;

import com.codeit.modoo_playlist.core.domain.notification.entity.NotificationLevel;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        Instant createdAt,
        UUID receiverId,
        String title,
        String content,
        NotificationLevel level,
        boolean isRead
) {
}