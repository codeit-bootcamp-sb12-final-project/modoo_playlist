package com.codeit.modoo_playlist.modulerealtime.event.listener;

import com.codeit.modoo_playlist.infra.event.kafka.SseKafkaEvent;
import com.codeit.modoo_playlist.modulerealtime.sse.SseService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SseKafkaListenerTest {

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
