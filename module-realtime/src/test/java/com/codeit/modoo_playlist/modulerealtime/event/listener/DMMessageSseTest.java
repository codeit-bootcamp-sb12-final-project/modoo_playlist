package com.codeit.modoo_playlist.modulerealtime.event.listener;

import com.codeit.modoo_playlist.core.domain.message.entity.MessageDto;
import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.infra.event.kafka.RealtimeKafkaPublisher;
import com.codeit.modoo_playlist.infra.event.kafka.SseKafkaEvent;
import com.codeit.modoo_playlist.modulerealtime.event.DMCreateEvent;
import com.codeit.modoo_playlist.modulerealtime.event.LocalRealtimeNotifier;
import com.codeit.modoo_playlist.modulerealtime.sse.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DMMessageSseTest {
    private final UUID receiverId = UUID.randomUUID();
    private final MessageDto message = new MessageDto(UUID.randomUUID(), UUID.randomUUID(),
            Instant.now(), null, null, "DM SSE test");

    @Test
    void sendsOnlyAfterCommit() {
        RealtimeNotifier notifier = mock(RealtimeNotifier.class);
        try (var context = context(notifier)) {
            transaction().executeWithoutResult(status -> {
                context.publishEvent(new DMCreateEvent(receiverId, message));
                verifyNoInteractions(notifier);
            });
            verify(notifier).notifySse(Set.of(receiverId), "direct-messages", message);
            verifyNoMoreInteractions(notifier);
        }
    }

    @Test
    void doesNotSendOnRollback() {
        RealtimeNotifier notifier = mock(RealtimeNotifier.class);
        try (var context = context(notifier)) {
            transaction().executeWithoutResult(status -> {
                context.publishEvent(new DMCreateEvent(receiverId, message));
                status.setRollbackOnly();
            });
            verifyNoInteractions(notifier);
        }
    }

    @Test
    void doesNotSendWithoutTransaction() {
        RealtimeNotifier notifier = mock(RealtimeNotifier.class);
        try (var context = context(notifier)) {
            context.publishEvent(new DMCreateEvent(receiverId, message));
            verifyNoInteractions(notifier);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void routesDmThroughKafkaToAllReceiverConnectionsOnly() throws Exception {
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        when(kafka.send(eq(RealtimeKafkaPublisher.SSE_TOPIC), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        var notifier = new LocalRealtimeNotifier(mock(SimpMessagingTemplate.class), kafka);
        try (var context = context(notifier)) {
            transaction().executeWithoutResult(status ->
                    context.publishEvent(new DMCreateEvent(receiverId, message)));
        }
        var eventCaptor = ArgumentCaptor.forClass(SseKafkaEvent.class);
        verify(kafka).send(eq(RealtimeKafkaPublisher.SSE_TOPIC), anyString(), eventCaptor.capture());
        var event = eventCaptor.getValue();
        assertThat(event.receiverIds()).containsExactly(receiverId);
        assertThat(event.eventName()).isEqualTo("direct-messages");
        assertThat(event.payload()).isEqualTo(message);

        var emitters = new SseEmitterRepository();
        var messages = new SseMessageRepository();
        ReflectionTestUtils.setField(messages, "eventQueueCapacity", 100);
        var first = mock(SseEmitter.class);
        var second = mock(SseEmitter.class);
        var unrelated = mock(SseEmitter.class);
        emitters.save(receiverId, first);
        emitters.save(receiverId, second);
        emitters.save(UUID.randomUUID(), unrelated);

        new SseKafkaListener(new SseService(emitters, messages)).onSseEvent(event);

        var builder = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(first).send(builder.capture());
        verify(second).send(any(SseEmitter.SseEventBuilder.class));
        verifyNoInteractions(unrelated);
        var parts = builder.getValue().build().stream().map(part -> part.getData()).toList();
        assertThat(parts).contains(message);
        assertThat(parts.stream().filter(String.class::isInstance).map(String.class::cast)
                .reduce("", String::concat)).contains("event:direct-messages", "id:");
    }

    private AnnotationConfigApplicationContext context(RealtimeNotifier notifier) {
        var context = new AnnotationConfigApplicationContext();
        context.registerBean(TransactionalEventListenerFactory.class);
        context.registerBean(RealtimeNotifier.class, () -> notifier);
        context.register(DMCreateEventListener.class);
        context.refresh();
        return context;
    }

    // Exercise Spring's real transaction synchronization without a database.
    private TransactionTemplate transaction() {
        return new TransactionTemplate(new AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object tx, TransactionDefinition definition) { }
            @Override protected void doCommit(DefaultTransactionStatus status) { }
            @Override protected void doRollback(DefaultTransactionStatus status) { }
        });
    }
}
