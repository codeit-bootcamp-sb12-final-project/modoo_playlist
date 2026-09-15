package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.service;

import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.moduleapi.dto.watchingsession.response.StartResult;
import com.codeit.modoo_playlist.moduleapi.dto.watchingsession.WatchingSessionChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchingSessionRegistry {

    private final WatchingSessionService watchingSessionService;
    private final RealtimeNotifier realtimeNotifier;

    private final Map<SubscriptionKey, SessionState> sessions = new ConcurrentHashMap<>();

    // endAll 과 새 start가 동시에 들어오는걸 방지
    private final Set<String> disconnected = ConcurrentHashMap.newKeySet();

    private record SubscriptionKey(
            String webSocketSessionId,
            String subscriptionId
    ) {
    }

    private static class SessionState {
        UUID id;                // null : start 진행 중
        boolean startFinished;  // JOIN 전송 완료
        boolean endRequested;   // start 중에 end 요청이 들어옴
        boolean ending;         // end 진행 중
    }

    public void start(
            UUID watcherId,
            UUID contentId,
            String webSocketSessionId,
            String subscriptionId
    ) {
        // 이미 끊어진 연결인지 확인
        if(disconnected.contains(webSocketSessionId)){
            return;
        }

        SubscriptionKey key =
                new SubscriptionKey(webSocketSessionId, subscriptionId);
        SessionState state = new SessionState();

        // 미리 자리 선점 (중복 방지)
        if (sessions.putIfAbsent(key,state) != null) {
            return;
        }

        // 그 사이에 연결이 끊겼는지 한번 더 확인
        if (disconnected.contains(webSocketSessionId)){
            sessions.remove(key,state);
            return;
        }

        StartResult result;
        try{
            result = watchingSessionService.start(watcherId, contentId);
        } catch(RuntimeException ex){
            sessions.remove(key, state);
            throw ex;
        }

        synchronized (state) {
            state.id = result.watchingSessionId();
        }

        boolean endAfterStart;
        try {
            // JOIN 알림
            result.changes().forEach(this::broadcast);
        } finally {
            synchronized (state) {
                state.startFinished = true; // 시작 완료
                endAfterStart = state.endRequested && !state.ending; // end 요청 확인
                if (endAfterStart) {
                    state.ending = true;
                }
            }

            if (endAfterStart) {
                finishEnd(key, state, result.watchingSessionId());
            }
        }
    }

    public void end(
            String webSocketSessionId,
            String subscriptionId
    ) {
        end(new SubscriptionKey(webSocketSessionId, subscriptionId));
    }

    public void endAll(String webSocketSessionId) {
        disconnected.add(webSocketSessionId);

        List<SubscriptionKey> keys = sessions.keySet().stream()
                .filter(key ->
                        key.webSocketSessionId().equals(webSocketSessionId))
                .toList();

        for (SubscriptionKey key : keys) {
            try {
                end(key);
            } catch (RuntimeException exception) {
                log.error("시청 세션 종료 실패: {}", key, exception);
            }
        }
    }

    private void end(SubscriptionKey key) {
        SessionState state = sessions.get(key);
        if (state == null) {
            return;
        }

        UUID id;
        synchronized (state) {
            if (!state.startFinished) {
                state.endRequested = true;
                return;
            }

            if (state.ending) {
                return;
            }

            state.ending = true;
            id = state.id;
        }

        finishEnd(key, state, id);
    }

    private void finishEnd(
            SubscriptionKey key,
            SessionState state,
            UUID id
    ) {
        Optional<WatchingSessionChange> change;
        try {
            // DB 호출은 상태 객체의 잠금 밖에서 실행한다.
            change = watchingSessionService.end(id);
        } catch (RuntimeException exception) {
            synchronized (state) {
                state.ending = false;
            }
            throw exception;
        }

        // 성공한 종료의 결과만 제거한다. 다른 상태로 교체됐다면 건드리지 않는다.
        sessions.remove(key, state);
        change.ifPresent(this::broadcast);
    }

    public Set<UUID> activeSessionIds() {
        Set<UUID> ids = new HashSet<>();

        for (SessionState state : sessions.values()) {
            synchronized (state) {
                if (state.id != null && !state.ending) {
                    ids.add(state.id);
                }
            }
        }

        return Set.copyOf(ids);
    }

    public void touchActiveSessions() {
        Set<UUID> ids = activeSessionIds();
        if (!ids.isEmpty()) {
            watchingSessionService.touch(ids);
        }
    }

    public void expireStaleSessions(Duration timeout) {
        Instant cutoff = Instant.now().minus(timeout);
        watchingSessionService.expireStaleSessions(cutoff)
                .forEach(this::broadcast);
    }

    private void broadcast(WatchingSessionChange change) {
        UUID contentId = change.watchingSession().content().id();
        String destination = "/sub/contents/" + contentId + "/watch";

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