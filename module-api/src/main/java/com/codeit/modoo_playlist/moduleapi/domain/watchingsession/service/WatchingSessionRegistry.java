package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.service;

import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.moduleapi.domain.search.event.WatcherCountChangedEvent;
import com.codeit.modoo_playlist.moduleapi.dto.watchingsession.response.StartResult;
import com.codeit.modoo_playlist.moduleapi.dto.watchingsession.WatchingSessionChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchingSessionRegistry {

    private static final int MAX_END_ATTEMPTS = 3;

    private final WatchingSessionService watchingSessionService;
    private final RealtimeNotifier realtimeNotifier;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<String, ConnectionState> connections =
            new ConcurrentHashMap<>();

    // 연결이 종료돼도 DB 종료에 실패한 상태는 재시도할 수 있게 유지한다.
    private final Set<SessionState> trackedSessions =
            ConcurrentHashMap.newKeySet();

    private static final class ConnectionState {
        final Map<String, SessionState> subscriptions = new HashMap<>();
        boolean closed;
    }

    private static final class SessionState {
        final ConnectionState connection;
        final String subscriptionId;

        // 아래 필드는 모두 synchronized(connection) 안에서 접근한다.
        UUID id;                // null : start 진행 중
        boolean startFinished;  // JOIN 전송 완료
        boolean endRequested;   // start 중에 end 요청이 들어옴
        boolean ending;         // end 진행 중

        // 재시도 횟수 상한 관련 필드
        int endAttempts;
        boolean endFailed;

        SessionState(ConnectionState connection, String subscriptionId) {
            this.connection = connection;
            this.subscriptionId = subscriptionId;
        }
    }

    public void connected(String webSocketSessionId) {
        connections.putIfAbsent(
                webSocketSessionId,
                new ConnectionState()
        );
    }

    public void start(
            UUID watcherId,
            UUID contentId,
            String webSocketSessionId,
            String subscriptionId
    ) {
        ConnectionState connection = connections.get(webSocketSessionId);

        // 연결 등록 이전 또는 연결 종료 이후의 시작 요청은 무시한다.
        if (connection == null) {
            return;
        }

        SessionState state;

        synchronized (connection) {
            if (connection.closed) {
                return;
            }

            SessionState current =
                    connection.subscriptions.get(subscriptionId);

            // 종료 요청이 없는 현재 구독만 중복 시작으로 본다.
            if (current != null && !current.endRequested) {
                return;
            }

            state = new SessionState(connection, subscriptionId);
            connection.subscriptions.put(subscriptionId, state);
            trackedSessions.add(state);
        }

        StartResult result;

        boolean succeeded = false;

        try {
            // DB 호출: 잠금 밖
            result = watchingSessionService.start(watcherId, contentId);
            succeeded = true;
        } finally{
            if(!succeeded) {
                forget(state);
            }
        }

        synchronized (connection) {
            state.id = result.watchingSessionId();
        }

        try {
            // 알림 호출: 잠금 밖
            result.changes().forEach(this::broadcast);
        } finally {
            synchronized (connection) {
                state.startFinished = true;
            }

            // 시작 중 종료 요청이 있었다면 여기서 이어서 처리한다.
            finishEndIfRequested(state);
        }
    }


    public void end(
            String webSocketSessionId,
            String subscriptionId
    ) {
        ConnectionState connection = connections.get(webSocketSessionId);

        if (connection == null) {
            return;
        }

        SessionState state;

        synchronized (connection) {
            state = connection.subscriptions.get(subscriptionId);

            if (state == null) {
                return;
            }

            // DB 종료가 실패해도 이 요청은 취소하지 않는다.
            state.endRequested = true;
        }

        finishEndIfRequested(state);
    }

    public void endAll(String webSocketSessionId) {
        ConnectionState connection = connections.get(webSocketSessionId);

        if (connection == null) {
            return;
        }

        List<SessionState> states;

        synchronized (connection) {
            connection.closed = true;
            states = List.copyOf(connection.subscriptions.values());

            // 모든 구독에 종료 의도를 먼저 기록한다.
            for (SessionState state : states) {
                state.endRequested = true;
            }

            connection.subscriptions.clear();
        }

        connections.remove(webSocketSessionId, connection);

        // DB 호출: 잠금 밖
        for (SessionState state : states) {
            finishEndIfRequested(state);
        }
    }

    private void finishEndIfRequested(SessionState state) {
        UUID id;

        synchronized (state.connection) {
            if (!state.endRequested
                    || !state.startFinished
                    || state.ending
                    || state.endFailed
            ) {
                return;
            }

            state.ending = true;
            state.endAttempts++;
            id = state.id;
        }

        Optional<WatchingSessionChange> change;

        try {
            // DB 호출: 잠금 밖
            change = watchingSessionService.end(id);
        } catch (RuntimeException exception) {
            int attempts;
            boolean terminal;

            synchronized (state.connection) {
                // 종료 의도는 유지하고, 다음 재시도만 허용한다.
                state.ending = false;
                attempts = state.endAttempts;
                terminal = attempts >= MAX_END_ATTEMPTS;
                state.endFailed = terminal;

                if (terminal) {
                    forget(state);
                    log.error(
                            "시청 세션 종료 최종 실패: sessionId={}, attempts={},status=END_FAILED",
                            id, attempts, exception
                    );
                } else {
                    log.warn(
                            "시청 세션 종료 실패, 재시도 예정: sessionId={}, attempts={}/{}",
                            id, attempts, MAX_END_ATTEMPTS, exception
                    );
                }
                return;
            }
        }

        forget(state);

        // 알림 호출: 잠금 밖
        change.ifPresent(this::broadcast);
    }

    private void forget(SessionState state) {
        synchronized (state.connection) {
            state.connection.subscriptions.remove(
                    state.subscriptionId,
                    state
            );
            trackedSessions.remove(state);
        }
    }

    public void retryPendingEnds() {
        for (SessionState state : trackedSessions) {
            finishEndIfRequested(state);
        }
    }

    private Set<UUID> activeSessionIds() {
        Set<UUID> ids = new HashSet<>();

        for (SessionState state : trackedSessions) {
            synchronized (state.connection) {
                if (state.id != null
                        && !state.endRequested
                        && !state.connection.closed) {
                    ids.add(state.id);
                }
            }
        }
        return ids;
    }

    public void touchActiveSessions() {
        Set<UUID> ids = activeSessionIds();

        if (!ids.isEmpty()) {
            // DB 호출: 잠금 밖
            watchingSessionService.touch(ids);
        }
    }

    public void expireStaleSessions(Duration timeout) {
        Instant cutoff = Instant.now().minus(timeout);

        // DB 및 알림 호출: 잠금 밖
        watchingSessionService.expireStaleSessions(cutoff)
                .forEach(this::broadcast);
    }

    private void broadcast(WatchingSessionChange change) {
        UUID contentId = change.watchingSession().content().id();
        String destination = "/sub/contents/" + contentId + "/watch";

        // ES 반영을 위한 이벤트 발행
        try {
            eventPublisher.publishEvent(new WatcherCountChangedEvent(contentId));
        } catch (RuntimeException exception) {
            log.error(
                "시청자 수 변경 이벤트 발행 실패: contentId={}, watcherCount={}",
                contentId,
                change.watcherCount(),
                exception
            );
        }

        try {
            realtimeNotifier.notifyStomp(destination, change);
        } catch (RuntimeException exception) {
            log.error(
                    "시청 변경 메시지 전송 실패: destination={}, type={}",
                    destination,
                    change.type(),
                    exception
            );
        }
    }
}