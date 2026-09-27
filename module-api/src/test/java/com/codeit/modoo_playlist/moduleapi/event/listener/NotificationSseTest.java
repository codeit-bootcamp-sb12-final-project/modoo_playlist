package com.codeit.modoo_playlist.moduleapi.event.listener;

import com.codeit.modoo_playlist.core.domain.notification.dto.NotificationResponse;
import com.codeit.modoo_playlist.core.domain.notification.entity.NotificationLevel;
import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.infra.event.kafka.SseKafkaEvent;
import com.codeit.modoo_playlist.moduleapi.event.NotificationCreatedEvent;
import com.codeit.modoo_playlist.moduleapi.sse.SseService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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

    @Test
    void kafkaListenerIsAvailableInDevAndForwardsNotifications() {
        SseService sseService = mock(SseService.class);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("dev");
            context.registerBean(SseService.class, () -> sseService);
            context.register(SseKafkaListener.class);
            context.refresh();
            SseKafkaEvent event = new SseKafkaEvent(Set.of(UUID.randomUUID()), "notifications", "payload");

            context.getBean(SseKafkaListener.class).onSseEvent(event);

            verify(sseService).send(event.receiverIds(), event.eventName(), event.payload());
        }
    }
}
