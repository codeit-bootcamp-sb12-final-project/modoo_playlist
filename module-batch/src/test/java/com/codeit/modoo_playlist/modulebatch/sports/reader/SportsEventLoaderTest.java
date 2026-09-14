package com.codeit.modoo_playlist.modulebatch.sports.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.infra.client.sportsdb.SportsDbClient;
import com.codeit.modoo_playlist.infra.client.sportsdb.dto.SportsDbEvent;
import com.codeit.modoo_playlist.modulebatch.sports.config.SportsBatchProperties;
import com.codeit.modoo_playlist.modulebatch.sports.config.SportsBatchProperties.League;
import com.codeit.modoo_playlist.modulebatch.sports.persistence.SportsContentMapper;

@ExtendWith(MockitoExtension.class)
class SportsEventLoaderTest {

    @Mock private SportsDbClient client;
    @Mock private SportsContentMapper mapper;

    private SportsEventLoader loader;

    @BeforeEach
    void setUp() {
        SportsBatchProperties properties = new SportsBatchProperties();
        properties.setDiscoveryDays(1);
        properties.setLeagues(List.of(new League("100", "league")));
        loader = new SportsEventLoader(client, mapper, properties);
    }

    @AfterEach
    void clearTransactionState() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void 발견결과는_빈아이디를_제외하고_같은아이디의_최신값으로_중복제거한다() {
        SportsDbEvent old = event("1", "old");
        SportsDbEvent latest = event("1", "latest");
        when(client.getEventsByDay(any(), eq("100"))).thenReturn(List.of(old, event(" ", "invalid"), latest));
        refreshIds(List.of());

        var result = loader.load();

        assertThat(result).containsExactly(latest);
    }

    @Test
    void 목록에서_누락된_DB경기는_아이디로_보완조회한다() {
        SportsDbEvent discovered = event("1", "found");
        SportsDbEvent refreshed = event("2", "refreshed");
        when(client.getEventsByDay(any(), eq("100"))).thenReturn(List.of(discovered));
        refreshIds(List.of("1", "2"));
        when(client.getEventById("2")).thenReturn(List.of(event("other", "wrong"), refreshed));

        assertThat(loader.load()).containsExactly(discovered, refreshed);
        verify(client, never()).getEventById("1");
    }

    @Test
    void 일부_보완조회_실패는_전체수집을_중단하지_않는다() {
        SportsDbEvent discovered = event("1", "found");
        when(client.getEventsByDay(any(), eq("100"))).thenReturn(List.of(discovered));
        refreshIds(List.of("2"));
        when(client.getEventById("2")).thenThrow(new RestClientException("temporary"));

        assertThat(loader.load()).containsExactly(discovered);
    }

    @Test
    void 모든_발견요청이_실패하면_잡을_실패시킨다() {
        when(client.getEventsByDay(any(), eq("100"))).thenThrow(new RestClientException("down"));

        assertThatThrownBy(loader::load)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SPORTSDB_DISCOVERY_FAILED));
    }

    @Test
    void 인증실패는_즉시_배치실패로_전환한다() {
        when(client.getEventsByDay(any(), eq("100")))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        assertThatThrownBy(loader::load)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SPORTSDB_AUTHENTICATION_FAILED));
    }

    @Test
    void 외부API조회는_DB트랜잭션_안에서_실행하지_않는다() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        assertThatThrownBy(loader::load)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("트랜잭션 밖");
        verify(client, never()).getEventsByDay(any(), any());
    }

    private void refreshIds(List<String> ids) {
        when(mapper.findRefreshSourceIds(any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(ids);
    }

    private SportsDbEvent event(String id, String title) {
        return new SportsDbEvent(id, title, "league", "season", "Soccer", "home", "away",
                "venue", "2026-09-14T12:00:00Z", "country", "thumb", "leagueBadge",
                "homeBadge", "Scheduled", "no");
    }
}
