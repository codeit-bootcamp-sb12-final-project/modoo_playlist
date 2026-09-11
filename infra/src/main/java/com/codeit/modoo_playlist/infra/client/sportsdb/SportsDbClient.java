package com.codeit.modoo_playlist.infra.client.sportsdb;

import java.time.LocalDate;
import java.util.List;

import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.infra.client.sportsdb.dto.SportsDbEvent;
import com.codeit.modoo_playlist.infra.client.sportsdb.dto.SportsDbEventResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SportsDbClient {

    private final RestClient restClient;
    private final SportsDbClientProperties properties;
    private final RetryTemplate retryTemplate;
    private final SportsDbRateLimiter rateLimiter;

    public List<SportsDbEvent> getEventsByDay(LocalDate date, String league) {
        return execute(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{apiKey}/eventsday.php")
                        .queryParam("d", date)
                        .queryParam("l", league)
                        .build(properties.getApiKey()))
                .retrieve()
                .body(SportsDbEventResponse.class));
    }

    public List<SportsDbEvent> getEventById(String eventId) {
        return execute(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{apiKey}/lookupevent.php")
                        .queryParam("id", eventId)
                        .build(properties.getApiKey()))
                .retrieve()
                .body(SportsDbEventResponse.class));
    }

    private List<SportsDbEvent> execute(java.util.function.Supplier<SportsDbEventResponse> request) {
        SportsDbEventResponse response = retryTemplate.invoke(() -> {
            rateLimiter.acquire();
            return request.get();
        });
        if (response == null) {
            throw new RestClientException("TheSportsDB 응답 본문이 없습니다.");
        }
        if (response.error() != null && !response.error().isBlank()) {
            throw new BaseException(ErrorCode.SPORTSDB_API_ERROR);
        }
        if (response.message() != null && !response.message().isBlank()) {
            throw new BaseException(ErrorCode.SPORTSDB_API_ERROR);
        }
        return response.events() == null ? List.of() : response.events();
    }
}
