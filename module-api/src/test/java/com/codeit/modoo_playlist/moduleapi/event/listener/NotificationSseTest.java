package com.codeit.modoo_playlist.moduleapi.event.listener;

import com.codeit.modoo_playlist.core.domain.notification.dto.NotificationResponse;
import com.codeit.modoo_playlist.core.domain.notification.entity.NotificationLevel;
import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.moduleapi.event.NotificationCreatedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationSseTest {

    @Test
    void publishesTheEventSubscribedByTheNotificationBadge() {
        RealtimeNotifier notifier = mock(RealtimeNotifier.class);
        NotificationResponse notification = new NotificationResponse(
                UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
                "title", "content", NotificationLevel.INFO, false);

        new NotificationCreatedEventListener(notifier)
                .onNotificationCreated(new NotificationCreatedEvent(notification));

        verify(notifier).notifySse(Set.of(notification.receiverId()), "notifications", notification);
    }

}
