package com.codeit.modoo_playlist.modulebatch.tmdb.reader;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.codeit.modoo_playlist.infra.client.tmdb.TmdbClient;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbMediaSummary;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbPageResponse;
import com.codeit.modoo_playlist.modulebatch.tmdb.config.TmdbBatchProperties;
import com.codeit.modoo_playlist.modulebatch.tmdb.exception.TmdbFatalIntegrationException;
import com.codeit.modoo_playlist.modulebatch.tmdb.exception.TmdbPathUnavailableException;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbCandidate;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbCandidate.MediaType;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbFetchedContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.persistence.TmdbContentMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TmdbCandidateLoader {

    private static final int MOVIE_ACTIVE_WEEKS = 4;

    private final TmdbClient tmdbClient;
    private final TmdbContentMapper contentMapper;
    private final TmdbBatchProperties properties;

    public List<TmdbFetchedContent> loadMovieActive() {
        assertOutsideTransaction();
        Set<Long> ids = new LinkedHashSet<>();
        for (int page = 1; page <= properties.getActiveMaxPages(); page++) {
            int currentPage = page;
            addMovieIds(ids, loadPage(
                    () -> tmdbClient.getMovieNowPlaying(currentPage),
                    "movie now_playing"
            ));
        }

        LocalDate today = LocalDate.now(ZoneId.of(properties.getZone()));
        contentMapper.findActiveMovieSourceIds(today.minusWeeks(MOVIE_ACTIVE_WEEKS), today).stream()
                .map(this::parseSourceId)
                .forEach(ids::add);
        return fetchDetails(ids, MediaType.MOVIE);
    }

    public List<TmdbFetchedContent> loadMoviePopular() {
        assertOutsideTransaction();
        Set<Long> ids = new LinkedHashSet<>();
        for (int page = 1; page <= properties.getPopularMaxPages(); page++) {
            int currentPage = page;
            addMovieIds(ids, loadPage(
                    () -> tmdbClient.getMoviePopular(currentPage),
                    "movie popular"
            ));
        }
        removeExisting(ids, MediaType.MOVIE);
        return fetchDetails(ids, MediaType.MOVIE);
    }

    public List<TmdbFetchedContent> loadTvActive() {
        assertOutsideTransaction();
        Set<Long> ids = new LinkedHashSet<>();
        for (int page = 1; page <= properties.getActiveMaxPages(); page++) {
            int currentPage = page;
            addTvIds(ids, loadPage(
                    () -> tmdbClient.getTvOnTheAir(currentPage),
                    "tv on_the_air"
            ));
        }

        contentMapper.findActiveTvSourceIds().stream()
                .map(this::parseSourceId)
                .forEach(ids::add);
        return fetchDetails(ids, MediaType.TV);
    }

    public List<TmdbFetchedContent> loadTvPopular() {
        assertOutsideTransaction();
        Set<Long> ids = new LinkedHashSet<>();
        for (int page = 1; page <= properties.getPopularMaxPages(); page++) {
            int currentPage = page;
            addTvIds(ids, loadPage(
                    () -> tmdbClient.getTvPopular(currentPage),
                    "tv popular"
            ));
        }
        removeExisting(ids, MediaType.TV);
        return fetchDetails(ids, MediaType.TV);
    }

    private TmdbPageResponse<TmdbMediaSummary> loadPage(PageRequest request, String pathName) {
        try {
            return request.load();
        } catch (RestClientException exception) {
            throw pathFailure(pathName, exception);
        }
    }

    private List<TmdbFetchedContent> fetchDetails(Set<Long> ids, MediaType mediaType) {
        List<TmdbFetchedContent> fetched = new ArrayList<>(ids.size());
        for (long id : ids) {
            TmdbCandidate candidate = new TmdbCandidate(id, mediaType);
            try {
                fetched.add(mediaType == MediaType.MOVIE
                        ? TmdbFetchedContent.movie(candidate, tmdbClient.getMovieDetail(id))
                        : TmdbFetchedContent.tv(candidate, tmdbClient.getTvDetail(id)));
            } catch (RestClientException exception) {
                if (authenticationFailure(exception)) {
                    throw new TmdbFatalIntegrationException("TMDB 인증에 실패했습니다.", exception);
                }
                fetched.add(TmdbFetchedContent.failed(candidate, exception));
            }
        }
        return fetched;
    }

    private void removeExisting(Set<Long> ids, MediaType mediaType) {
        if (ids.isEmpty()) {
            return;
        }
        List<String> sourceIds = ids.stream().map(String::valueOf).toList();
        Set<String> existingIds = Set.copyOf(contentMapper.findExistingSourceIds(mediaType.name(), sourceIds));
        ids.removeIf(id -> existingIds.contains(Long.toString(id)));
    }

    private void addMovieIds(Set<Long> ids, TmdbPageResponse<TmdbMediaSummary> page) {
        page.results().stream()
                .filter(summary -> summary != null && !summary.adult())
                .map(TmdbMediaSummary::id)
                .forEach(ids::add);
    }

    private void addTvIds(Set<Long> ids, TmdbPageResponse<TmdbMediaSummary> page) {
        page.results().stream()
                .filter(summary -> summary != null && allowedTvGenre(summary))
                .map(TmdbMediaSummary::id)
                .forEach(ids::add);
    }

    private boolean allowedTvGenre(TmdbMediaSummary summary) {
        return summary.genreIds() == null
                || summary.genreIds().stream().noneMatch(properties.getExcludedTvGenreIds()::contains);
    }

    private long parseSourceId(String sourceId) {
        try {
            return Long.parseLong(sourceId);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("DB에 잘못된 TMDB source_id가 있습니다: " + sourceId, exception);
        }
    }

    private RuntimeException pathFailure(String pathName, RestClientException exception) {
        if (authenticationFailure(exception)) {
            return new TmdbFatalIntegrationException("TMDB 인증에 실패했습니다.", exception);
        }
        return new TmdbPathUnavailableException("TMDB " + pathName + " 목록을 가져오지 못했습니다.", exception);
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
            throw new IllegalStateException("TMDB 외부 API 조회는 DB 트랜잭션 밖에서 실행해야 합니다.");
        }
    }

    @FunctionalInterface
    private interface PageRequest {
        TmdbPageResponse<TmdbMediaSummary> load();
    }
}
