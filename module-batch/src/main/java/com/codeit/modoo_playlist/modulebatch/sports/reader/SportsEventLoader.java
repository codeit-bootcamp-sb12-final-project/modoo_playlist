package com.codeit.modoo_playlist.modulebatch.sports.reader;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.infra.client.sportsdb.SportsDbClient;
import com.codeit.modoo_playlist.infra.client.sportsdb.dto.SportsDbEvent;
import com.codeit.modoo_playlist.modulebatch.sports.config.SportsBatchProperties;
import com.codeit.modoo_playlist.modulebatch.sports.config.SportsBatchProperties.League;
import com.codeit.modoo_playlist.modulebatch.sports.persistence.SportsContentMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SportsEventLoader {

    private final SportsDbClient sportsDbClient;
    private final SportsContentMapper contentMapper;
    private final SportsBatchProperties properties;

    public List<SportsDbEvent> load() {
        assertOutsideTransaction();

        ZoneId zone = ZoneId.of(properties.getZone());
        LocalDate today = LocalDate.now(zone);
        Map<String, SportsDbEvent> eventsById = new LinkedHashMap<>();
        int successfulRequests = discover(today, eventsById);

        if (successfulRequests == 0) {
            throw new BaseException(ErrorCode.SPORTSDB_DISCOVERY_FAILED);
        }

        refreshMissingEvents(today, zone, eventsById);
        return new ArrayList<>(eventsById.values());
    }

    private int discover(LocalDate today, Map<String, SportsDbEvent> eventsById) {
        int successfulRequests = 0;
        for (int dayOffset = 0; dayOffset < properties.getDiscoveryDays(); dayOffset++) {
            LocalDate date = today.plusDays(dayOffset);
            for (League league : properties.getLeagues()) {
                try {
                    List<SportsDbEvent> events = sportsDbClient.getEventsByDay(date, league.id());
                    successfulRequests++;
                    addValidIds(eventsById, events);
                } catch (BaseException exception) {
                    throw exception;
                } catch (RestClientException exception) {
                    if (authenticationFailure(exception)) {
                        throw new BaseException(ErrorCode.SPORTSDB_AUTHENTICATION_FAILED, exception);
                    }
                    log.warn(
                            "TheSportsDB Discovery 요청을 건너뜁니다. date={}, league={}, reason={}",
                            date,
                            league.name() + "(" + league.id() + ")",
                            exception.getMessage()
                    );
                }
            }
        }
        return successfulRequests;
    }

    private void refreshMissingEvents(
            LocalDate today,
            ZoneId zone,
            Map<String, SportsDbEvent> eventsById
    ) {
        Instant now = Instant.now();
        Instant windowStart = today.atStartOfDay(zone).toInstant();
        Instant windowEnd = today.plusDays(properties.getDiscoveryDays()).atStartOfDay(zone).toInstant();
        List<String> sourceIds = contentMapper.findRefreshSourceIds(
                windowStart,
                windowEnd,
                now,
                properties.getSoccerDurationMinutes(),
                properties.getBasketballDurationMinutes(),
                properties.getBaseballDurationMinutes(),
                properties.getDefaultDurationMinutes()
        );

        for (String sourceId : sourceIds) {
            if (eventsById.containsKey(sourceId)) {
                continue;
            }
            try {
                List<SportsDbEvent> refreshed = sportsDbClient.getEventById(sourceId);
                SportsDbEvent event = refreshed.stream()
                        .filter(item -> item != null && sourceId.equals(item.idEvent()))
                        .findFirst()
                        .orElse(null);
                if (event == null) {
                    log.warn("TheSportsDB 보완조회 결과가 없습니다. idEvent={}", sourceId);
                    continue;
                }
                eventsById.put(sourceId, event);
            } catch (BaseException exception) {
                throw exception;
            } catch (RestClientException exception) {
                if (authenticationFailure(exception)) {
                    throw new BaseException(ErrorCode.SPORTSDB_AUTHENTICATION_FAILED, exception);
                }
                log.warn(
                        "TheSportsDB 보완조회를 건너뜁니다. idEvent={}, reason={}",
                        sourceId,
                        exception.getMessage()
                );
            }
        }
    }

    private void addValidIds(Map<String, SportsDbEvent> eventsById, List<SportsDbEvent> events) {
        for (SportsDbEvent event : events) {
            if (event != null && event.idEvent() != null && !event.idEvent().isBlank()) {
                eventsById.put(event.idEvent(), event);
            }
        }
    }

    private boolean authenticationFailure(RestClientException exception) {
        if (!(exception instanceof RestClientResponseException responseException)) {
            return false;
        }
        return responseException.getStatusCode() == HttpStatus.UNAUTHORIZED
                || responseException.getStatusCode() == HttpStatus.FORBIDDEN;
    }

    private void assertOutsideTransaction() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("TheSportsDB 외부 API 조회는 DB 트랜잭션 밖에서 실행해야 합니다.");
        }
    }
}
