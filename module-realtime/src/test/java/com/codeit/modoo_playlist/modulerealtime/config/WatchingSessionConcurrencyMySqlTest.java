package com.codeit.modoo_playlist.modulerealtime.config;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.infra.config.JpaConfig;
import com.codeit.modoo_playlist.infra.config.QuerydslConfig;
import com.codeit.modoo_playlist.infra.event.kafka.WatcherCountChangedEvent;
import com.codeit.modoo_playlist.infra.repository.watchingsession.WatchingSessionRepository;
import com.codeit.modoo_playlist.modulerealtime.dto.watchingsession.ChangeType;
import com.codeit.modoo_playlist.modulerealtime.dto.watchingsession.WatchingSessionChange;
import com.codeit.modoo_playlist.modulerealtime.watchingSession.service.WatchingSessionCommandService;
import com.codeit.modoo_playlist.modulerealtime.watchingSession.service.WatchingSessionRegistry;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual MySQL row locks and separate transactions; only outbound transports are mocked. */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:tc:mysql:8.4.7:///watching_concurrency",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../infra/src/main/resources/schema.sql",
        "spring.test.database.replace=NONE",
        "spring.jpa.show-sql=false"
})
@ContextConfiguration(classes = WatchingSessionConcurrencyMySqlTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WatchingSessionConcurrencyMySqlTest {
    @Configuration(proxyBeanMethods = false)
    @EntityScan("com.codeit.modoo_playlist.core")
    @EnableJpaRepositories("com.codeit.modoo_playlist.infra.repository")
    @ComponentScan("com.codeit.modoo_playlist.infra.mapper")
    @Import({JpaConfig.class, QuerydslConfig.class, WatchingSessionCommandService.class})
    static class Config {}

    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired WatchingSessionRepository sessions;
    @Autowired WatchingSessionCommandService commands;

    private TransactionTemplate transaction;
    private RealtimeNotifier notifier;
    private KafkaTemplate<String, WatcherCountChangedEvent> kafka;
    private WatchingSessionRegistry firstServer;
    private WatchingSessionRegistry secondServer;
    private UUID sessionId;
    private UUID contentId;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void createStaleSession() {
        transaction = new TransactionTemplate(transactionManager);
        notifier = mock(RealtimeNotifier.class);
        kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), anyString(), any(WatcherCountChangedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        firstServer = registry();
        secondServer = registry();
        UUID watcherId = transaction.execute(status -> {
            User user = User.create(UUID.randomUUID() + "@example.com", "watcher", "password");
            Content content = Content.builder().type(ContentType.MOVIE).title("Concurrent watching").build();
            em.persist(user);
            em.persist(content);
            contentId = content.getId();
            return user.getId();
        });
        firstServer.connected("connection");
        firstServer.start(watcherId, contentId, "connection", "subscription");
        sessionId = sessions.findByWatcher_IdAndEndedAtIsNull(watcherId).get(0).getId();
        transaction.executeWithoutResult(status -> em.createQuery(
                "update WatchingSession s set s.updatedAt = :stale where s.id = :id")
                .setParameter("stale", Instant.now().minusSeconds(120))
                .setParameter("id", sessionId).executeUpdate());
        clearInvocations(notifier, kafka); // Ignore the initial JOIN.
    }

    @Test
    void twoServersExpiringTheSameSessionSendOnlyOneLeave() throws Exception {
        race(() -> firstServer.expireStaleSessions(Duration.ofSeconds(60)),
                () -> secondServer.expireStaleSessions(Duration.ofSeconds(60)));
        assertSingleLeave();
    }

    @Test
    void expirationRacingWithDisconnectSendsOnlyOneLeave() throws Exception {
        race(() -> firstServer.end("connection", "subscription"),
                () -> secondServer.expireStaleSessions(Duration.ofSeconds(60)));
        assertSingleLeave();
    }

    private WatchingSessionRegistry registry() {
        return new WatchingSessionRegistry(commands, notifier, mock(ApplicationEventPublisher.class), kafka);
    }

    private void race(Runnable first, Runnable second) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch started = new CountDownLatch(2);
        try {
            // Hold the row until both independent transactions have attempted their operation.
            var futures = transaction.execute(status -> {
                sessions.findWatchingSessionById(sessionId).orElseThrow();
                Future<?> a = workers.submit(() -> { started.countDown(); first.run(); });
                Future<?> b = workers.submit(() -> { started.countDown(); second.run(); });
                try {
                    assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
                    assertThrows(TimeoutException.class, () -> a.get(300, TimeUnit.MILLISECONDS));
                    assertThrows(TimeoutException.class, () -> b.get(300, TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                return java.util.List.of(a, b);
            }); // Commit releases the lock; the two workers now compete for it.
            for (Future<?> future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }
        } finally {
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void assertSingleLeave() {
        WatchingSession ended = sessions.findById(sessionId).orElseThrow();
        assertThat(ended.getEndedAt()).isNotNull();
        assertThat(sessions.countDistinctByContent_IdAndEndedAtIsNull(contentId)).isZero();
        var payload = ArgumentCaptor.forClass(WatchingSessionChange.class);
        verify(notifier).notifyStomp(eq("/sub/contents/" + contentId + "/watch"), payload.capture());
        assertThat(payload.getValue().type()).isEqualTo(ChangeType.LEAVE);
        assertThat(payload.getValue().watchingSession().id()).isEqualTo(sessionId);
        assertThat(payload.getValue().watcherCount()).isZero();
        verifyNoMoreInteractions(notifier);
        verify(kafka).send(eq(WatcherCountChangedEvent.TOPIC), eq(contentId.toString()),
                any(WatcherCountChangedEvent.class));
        verifyNoMoreInteractions(kafka);
        assertThat(commands.end(sessionId)).isEmpty();
        assertThat(commands.expireStaleSessions(Instant.now())).isEmpty();
    }
}
