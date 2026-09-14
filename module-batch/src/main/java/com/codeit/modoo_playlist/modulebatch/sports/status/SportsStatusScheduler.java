package com.codeit.modoo_playlist.modulebatch.sports.status;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "batch.sports",
        name = "status-update-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SportsStatusScheduler {

    private final SportsStatusUpdateService statusUpdateService;

    @Scheduled(
            cron = "${batch.sports.status-cron:0 */15 * * * *}",
            zone = "${batch.sports.zone:Asia/Seoul}"
    )
    public void updateStatuses() {
        int updated = statusUpdateService.updateStatuses();
        if (updated > 0) {
            log.info("스포츠 경기 상태를 갱신했습니다. updated={}", updated);
        }
    }
}
