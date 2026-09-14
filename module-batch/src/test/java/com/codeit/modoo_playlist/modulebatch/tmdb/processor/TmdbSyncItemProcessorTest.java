package com.codeit.modoo_playlist.modulebatch.tmdb.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.ExistingTmdbContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbCandidate;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbFetchedContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbSyncContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.persistence.TmdbContentMapper;
import com.codeit.modoo_playlist.modulebatch.tmdb.processor.TmdbSyncItemProcessor.SyncMode;

@ExtendWith(MockitoExtension.class)
class TmdbSyncItemProcessorTest {

    @Mock private TmdbContentMapper mapper;
    @Mock private TmdbSyncConverter converter;

    @Test
    void 상세조회_실패는_skip가능한_에러코드로_변환한다() {
        TmdbCandidate candidate = new TmdbCandidate(10, TmdbCandidate.MediaType.MOVIE);
        TmdbSyncItemProcessor processor = processor(TmdbCandidate.MediaType.MOVIE, SyncMode.ACTIVE);

        assertThatThrownBy(() -> processor.process(TmdbFetchedContent.failed(candidate, new RuntimeException())))
                .isInstanceOfSatisfying(BaseException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TMDB_ITEM_FETCH_FAILED);
                    assertThat(exception.getDetails()).containsEntry("tmdbId", 10L);
                });
    }

    @Test
    void POPULAR는_이미_존재하거나_삭제된_콘텐츠를_변환하지_않는다() throws Exception {
        TmdbCandidate candidate = new TmdbCandidate(10, TmdbCandidate.MediaType.MOVIE);
        when(mapper.findBySourceId("MOVIE", "10")).thenReturn(existing(null));

        assertThat(processor(TmdbCandidate.MediaType.MOVIE, SyncMode.POPULAR)
                .process(TmdbFetchedContent.movie(candidate, null))).isNull();
        verify(converter, never()).fromMovie(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ACTIVE도_softDelete된_콘텐츠는_건너뛴다() throws Exception {
        TmdbCandidate candidate = new TmdbCandidate(10, TmdbCandidate.MediaType.MOVIE);
        when(mapper.findBySourceId("MOVIE", "10")).thenReturn(existing(Instant.now()));

        assertThat(processor(TmdbCandidate.MediaType.MOVIE, SyncMode.ACTIVE)
                .process(TmdbFetchedContent.movie(candidate, null))).isNull();
    }

    @Test
    void POPULAR의_신규_UPCOMING은_저장하지_않는다() throws Exception {
        TmdbCandidate candidate = new TmdbCandidate(10, TmdbCandidate.MediaType.MOVIE);
        TmdbSyncContent converted = sync("UPCOMING");
        when(mapper.findBySourceId("MOVIE", "10")).thenReturn(null);
        when(converter.fromMovie(null, null)).thenReturn(converted);

        assertThat(processor(TmdbCandidate.MediaType.MOVIE, SyncMode.POPULAR)
                .process(TmdbFetchedContent.movie(candidate, null))).isNull();
    }

    @Test
    void ACTIVE의_신규콘텐츠는_변환결과를_반환한다() throws Exception {
        TmdbCandidate candidate = new TmdbCandidate(10, TmdbCandidate.MediaType.MOVIE);
        TmdbSyncContent converted = sync("ONGOING");
        when(mapper.findBySourceId("MOVIE", "10")).thenReturn(null);
        when(converter.fromMovie(null, null)).thenReturn(converted);

        assertThat(processor(TmdbCandidate.MediaType.MOVIE, SyncMode.ACTIVE)
                .process(TmdbFetchedContent.movie(candidate, null))).isSameAs(converted);
    }

    private TmdbSyncItemProcessor processor(TmdbCandidate.MediaType type, SyncMode mode) {
        return new TmdbSyncItemProcessor(type, mode, mapper, converter);
    }

    private ExistingTmdbContent existing(Instant deletedAt) {
        return new ExistingTmdbContent("id", "title", null, null, deletedAt,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private TmdbSyncContent sync(String status) {
        return new TmdbSyncContent("id", "MOVIE", "title", null, null, "10", null, null,
                new TmdbSyncContent.Video(null, null, null, status, null, null, null, null, null, null),
                List.of(), List.of(), true, true);
    }
}
