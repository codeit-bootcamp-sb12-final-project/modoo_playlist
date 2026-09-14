package com.codeit.modoo_playlist.modulebatch.tmdb.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.infra.client.tmdb.TmdbClient;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbGenreResponse;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbKeywordResponse;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbMovieDetailResponse;
import com.codeit.modoo_playlist.modulebatch.tmdb.config.TmdbBatchProperties;

@ExtendWith(MockitoExtension.class)
class TmdbSyncConverterTest {

    @Mock private TmdbClient client;

    @Test
    void 영화상세를_콘텐츠_비디오_장르와_키워드로_변환한다() {
        when(client.toImageUrl("/poster.jpg")).thenReturn("https://image/poster.jpg");
        TmdbSyncConverter converter = new TmdbSyncConverter(client, new TmdbBatchProperties());
        TmdbMovieDetailResponse detail = movie("Movie", "/poster.jpg",
                List.of(new TmdbGenreResponse("Action"), new TmdbGenreResponse("action")),
                new TmdbKeywordResponse(List.of(new TmdbKeywordResponse.Keyword("Hero")), null));

        var result = converter.fromMovie(detail, null);

        assertThat(result.type()).isEqualTo("MOVIE");
        assertThat(result.sourceId()).isEqualTo("10");
        assertThat(result.thumbnailUrl()).isEqualTo("https://image/poster.jpg");
        assertThat(result.video().releaseStatus()).isEqualTo("RELEASED");
        assertThat(result.tags()).extracting(tag -> tag.name()).containsExactly("Action", "Hero");
        assertThat(result.replacePeople()).isTrue();
        assertThat(result.replaceTags()).isTrue();
    }

    @Test
    void 신규콘텐츠는_제목과_포스터가_필수다() {
        TmdbSyncConverter converter = new TmdbSyncConverter(client, new TmdbBatchProperties());

        assertInvalid(() -> converter.fromMovie(movie(null, "/poster.jpg", List.of(), keywords()), null), "title");
        assertInvalid(() -> converter.fromMovie(movie("Movie", null, List.of(), keywords()), null), "posterPath");
    }

    @Test
    void 미래개봉일은_UPCOMING으로_계산한다() {
        when(client.toImageUrl("/poster.jpg")).thenReturn("image");
        TmdbSyncConverter converter = new TmdbSyncConverter(client, new TmdbBatchProperties());
        TmdbMovieDetailResponse detail = new TmdbMovieDetailResponse(
                10, "Movie", null, "/poster.jpg", LocalDate.now().plusDays(10),
                List.of("US"), "en", 1f, BigDecimal.ONE, 1, 100, null,
                "Released", null, List.of(), null, keywords()
        );

        assertThat(converter.fromMovie(detail, null).video().releaseStatus()).isEqualTo("UPCOMING");
    }

    private TmdbMovieDetailResponse movie(
            String title, String poster, List<TmdbGenreResponse> genres, TmdbKeywordResponse keywords
    ) {
        return new TmdbMovieDetailResponse(10, title, "overview", poster, LocalDate.of(2020, 1, 1),
                List.of("US"), "en", 1f, BigDecimal.TEN, 100, 120, "tt10",
                "Released", null, genres, null, keywords);
    }

    private TmdbKeywordResponse keywords() {
        return new TmdbKeywordResponse(List.of(), null);
    }

    private void assertInvalid(Runnable action, String field) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BaseException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TMDB_CONTENT_INVALID);
            assertThat(exception.getDetails()).containsEntry("field", field);
        });
    }
}
