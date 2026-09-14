package com.codeit.modoo_playlist.modulebatch.tmdb.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import com.codeit.modoo_playlist.infra.client.tmdb.TmdbClient;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbMediaSummary;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbMovieDetailResponse;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbPageResponse;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbTvDetailResponse;
import com.codeit.modoo_playlist.modulebatch.tmdb.config.TmdbBatchProperties;
import com.codeit.modoo_playlist.modulebatch.tmdb.persistence.TmdbContentMapper;

@ExtendWith(MockitoExtension.class)
class TmdbCandidateLoaderTest {

    @Mock private TmdbClient client;
    @Mock private TmdbContentMapper mapper;

    private TmdbBatchProperties properties;
    private TmdbCandidateLoader loader;

    @BeforeEach
    void setUp() {
        properties = new TmdbBatchProperties();
        properties.setActiveMaxPages(1);
        properties.setPopularMaxPages(1);
        loader = new TmdbCandidateLoader(client, mapper, properties);
    }

    @AfterEach
    void clearTransactionState() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void 인기영화는_성인물과_중복아이디와_DB기존콘텐츠를_제외한다() {
        var detail = mock(TmdbMovieDetailResponse.class);
        when(client.getMoviePopular(1)).thenReturn(page(
                movie(1, false), movie(1, false), movie(2, true), movie(3, false)));
        when(mapper.findExistingSourceIds(eq("MOVIE"), anyList())).thenReturn(List.of("3"));
        when(client.getMovieDetail(1)).thenReturn(detail);

        var result = loader.loadMoviePopular();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).candidate().tmdbId()).isEqualTo(1);
        verify(client, never()).getMovieDetail(2);
        verify(client, never()).getMovieDetail(3);
    }

    @Test
    void 활성영화는_DB의_갱신대상도_상세조회한다() {
        when(client.getMovieNowPlaying(1)).thenReturn(page(movie(1, false)));
        when(mapper.findActiveMovieSourceIds(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of("2"));
        when(client.getMovieDetail(1)).thenReturn(mock(TmdbMovieDetailResponse.class));
        when(client.getMovieDetail(2)).thenReturn(mock(TmdbMovieDetailResponse.class));

        assertThat(loader.loadMovieActive()).extracting(item -> item.candidate().tmdbId())
                .containsExactly(1L, 2L);
    }

    @Test
    void TV는_제외장르를_건너뛴다() {
        when(client.getTvPopular(1)).thenReturn(page(
                new TmdbMediaSummary(1, false, List.of(10763)),
                new TmdbMediaSummary(2, false, List.of(18))));
        when(mapper.findExistingSourceIds(eq("TV"), anyList())).thenReturn(List.of());
        when(client.getTvDetail(2)).thenReturn(mock(TmdbTvDetailResponse.class));

        assertThat(loader.loadTvPopular()).extracting(item -> item.candidate().tmdbId())
                .containsExactly(2L);
        verify(client, never()).getTvDetail(1);
    }

    @Test
    void 상세조회_일시실패는_실패항목으로_남기고_다음항목을_계속한다() {
        when(client.getMoviePopular(1)).thenReturn(page(movie(1, false), movie(2, false)));
        when(mapper.findExistingSourceIds(eq("MOVIE"), anyList())).thenReturn(List.of());
        when(client.getMovieDetail(1)).thenThrow(new RestClientException("temporary"));
        when(client.getMovieDetail(2)).thenReturn(mock(TmdbMovieDetailResponse.class));

        var result = loader.loadMoviePopular();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).failure()).isNotNull();
        assertThat(result.get(1).failure()).isNull();
    }

    @Test
    void 인증실패는_즉시_배치실패로_전환한다() {
        when(client.getMoviePopular(1)).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(loader::loadMoviePopular)
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.TMDB_AUTHENTICATION_FAILED));
    }

    @Test
    void 외부API조회는_DB트랜잭션_안에서_실행하지_않는다() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        assertThatThrownBy(loader::loadMoviePopular)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("트랜잭션 밖");
        verify(client, never()).getMoviePopular(1);
    }

    private TmdbMediaSummary movie(long id, boolean adult) {
        return new TmdbMediaSummary(id, adult, List.of());
    }

    @SafeVarargs
    private final TmdbPageResponse<TmdbMediaSummary> page(TmdbMediaSummary... items) {
        return new TmdbPageResponse<>(List.of(items));
    }
}
