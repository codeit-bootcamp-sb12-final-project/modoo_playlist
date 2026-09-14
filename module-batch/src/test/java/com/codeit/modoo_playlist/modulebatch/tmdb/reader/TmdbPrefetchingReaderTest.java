package com.codeit.modoo_playlist.modulebatch.tmdb.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbCandidate;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbFetchedContent;

class TmdbPrefetchingReaderTest {

    @Test
    void Step시작에_한번만_적재하고_순서대로_읽은_뒤_null을_반환한다() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        TmdbFetchedContent first = TmdbFetchedContent.failed(
                new TmdbCandidate(1, TmdbCandidate.MediaType.MOVIE), new RuntimeException()
        );
        TmdbFetchedContent second = TmdbFetchedContent.failed(
                new TmdbCandidate(2, TmdbCandidate.MediaType.MOVIE), new RuntimeException()
        );
        TmdbPrefetchingReader reader = new TmdbPrefetchingReader(() -> {
            calls.incrementAndGet();
            return List.of(first, second);
        });

        reader.beforeStep(null);

        assertThat(reader.read()).isSameAs(first);
        assertThat(reader.read()).isSameAs(second);
        assertThat(reader.read()).isNull();
        assertThat(calls).hasValue(1);
    }
}
