package com.codeit.modoo_playlist.moduleapi.event;

import com.codeit.modoo_playlist.core.domain.notification.dto.NotificationResponse;

public record NotificationCreatedEvent (
        NotificationResponse notification
){
}
