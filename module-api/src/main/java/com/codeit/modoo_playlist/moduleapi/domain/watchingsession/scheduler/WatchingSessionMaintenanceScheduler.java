package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.scheduler;

import java.time.Duration;

import com.codeit.modoo_playlist.moduleapi.domain.watchingsession.service.WatchingSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchingSessionMaintenanceScheduler {

    private static final Duration SESSION_TIMEOUT =
            Duration.ofSeconds(60);

    private final WatchingSessionRegistry registry;

    @Scheduled(fixedDelay = 10_000)
    public void touchActiveSessions() {
        try {
            registry.touchActiveSessions();
        } catch (RuntimeException exception) {
            log.error("시청 세션 heartbeat 갱신 실패", exception);
        }
    }

    @Scheduled(initialDelay = 0, fixedDelay = 15_000)
    public void expireStaleSessions() {
        try {
            registry.expireStaleSessions(SESSION_TIMEOUT);
        } catch (RuntimeException exception) {
            log.error("오래된 시청 세션 정리 실패", exception);
        }
    }
}