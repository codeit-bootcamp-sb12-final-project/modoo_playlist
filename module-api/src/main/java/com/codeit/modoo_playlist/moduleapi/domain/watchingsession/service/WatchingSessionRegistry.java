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

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchingSessionRegistry {

    private final WatchingSessionService watchingSessionService;
    private final RealtimeNotifier realtimeNotifier;

    private final Map<SubscriptionKey, UUID> sessions = new HashMap<>();

    private record SubscriptionKey(
            String webSocketSessionId,
            String subscriptionId
    ) {
    }

    public synchronized void start(
            UUID watcherId,
            UUID contentId,
            String webSocketSessionId,
            String subscriptionId
    ) {
        SubscriptionKey key =
                new SubscriptionKey(webSocketSessionId, subscriptionId);

        // 동일 연결에서 동일 subscription ID가 다시 들어오면 무시한다.
        if (sessions.containsKey(key)) {
            return;
        }

        // 별도 Spring Bean의 @Transactional 메서드이므로,
        // 정상 반환 시 DB 커밋이 완료된다.
        StartResult result =
                watchingSessionService.start(watcherId, contentId);

        sessions.put(key, result.watchingSessionId());

        // DB 커밋과 메모리 등록 후 전송한다.
        result.changes().forEach(this::broadcast);
    }

    public synchronized void end(
            String webSocketSessionId,
            String subscriptionId
    ) {
        end(new SubscriptionKey(webSocketSessionId, subscriptionId));
    }

    public synchronized void endAll(String webSocketSessionId) {
        List<SubscriptionKey> keys = sessions.keySet().stream()
                .filter(key ->
                        key.webSocketSessionId().equals(webSocketSessionId))
                .toList();

        for (SubscriptionKey key : keys) {
            try {
                end(key);
            } catch (RuntimeException exception) {
                // 하나의 실패로 나머지 구독 정리가 중단되지 않게 한다.
                log.error("시청 세션 종료 실패: {}", key, exception);
            }
        }
    }

    private void end(SubscriptionKey key) {
        UUID watchingSessionId = sessions.get(key);

        if (watchingSessionId == null) {
            return;
        }

        Optional<WatchingSessionChange> change =
                watchingSessionService.end(watchingSessionId);

        // DB 종료가 성공한 뒤에만 연결 정보를 제거한다.
        sessions.remove(key);

        change.ifPresent(this::broadcast);
    }

    private void broadcast(WatchingSessionChange change) {
        UUID contentId = change.watchingSession().content().id();

        String destination =
                "/sub/contents/" + contentId + "/watch";

        try {
            realtimeNotifier.notifyStomp(destination, change);
        } catch (RuntimeException exception) {
            // 전송 실패로 이미 커밋된 시청 상태를 되돌리지는 않는다.
            log.error(
                    "시청 변경 메시지 전송 실패: destination={}, type={}",
                    destination,
                    change.type(),
                    exception
            );
        }
    }

    public synchronized Set<UUID> activeSessionIds() {
        return Set.copyOf(sessions.values());
    }

    public void touchActiveSessions() {
        Set<UUID> sessionIds = activeSessionIds();

        if (!sessionIds.isEmpty()) {
            watchingSessionService.touch(sessionIds);
        }
    }

    public void expireStaleSessions(Duration timeout) {
        Instant cutoff = Instant.now().minus(timeout);

        watchingSessionService.expireStaleSessions(cutoff)
                .forEach(this::broadcast);
    }
}