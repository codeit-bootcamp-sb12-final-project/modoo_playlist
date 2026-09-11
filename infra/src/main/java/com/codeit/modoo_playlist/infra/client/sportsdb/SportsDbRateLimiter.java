package com.codeit.modoo_playlist.infra.client.sportsdb;

import org.springframework.web.client.RestClientException;

final class SportsDbRateLimiter {

    private final long intervalMillis;
    private long nextRequestAt;

    SportsDbRateLimiter(int requestsPerMinute) {
        this.intervalMillis = 60_000L / requestsPerMinute;
    }

    synchronized void acquire() {
        long now = System.currentTimeMillis();
        long waitMillis = nextRequestAt - now;
        if (waitMillis > 0) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RestClientException("TheSportsDB 호출 대기 중 스레드가 중단됐습니다.", exception);
            }
        }
        nextRequestAt = System.currentTimeMillis() + intervalMillis;
    }
}
