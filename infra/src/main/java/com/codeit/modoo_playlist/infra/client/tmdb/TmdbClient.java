package com.codeit.modoo_playlist.infra.client.tmdb;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbMediaSummary;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbMovieDetailResponse;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbPageResponse;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbTvDetailResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TmdbClient {

    private static final ParameterizedTypeReference<TmdbPageResponse<TmdbMediaSummary>> PAGE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final TmdbClientProperties properties;
    private final RetryTemplate retryTemplate;

    public TmdbPageResponse<TmdbMediaSummary> getMovieNowPlaying(int page) {
        return getPage("/movie/now_playing", page, true);
    }

    public TmdbPageResponse<TmdbMediaSummary> getMoviePopular(int page) {
        return getPage("/movie/popular", page, false);
    }

    public TmdbPageResponse<TmdbMediaSummary> getTvOnTheAir(int page) {
        return getPage("/tv/on_the_air", page, false);
    }

    public TmdbPageResponse<TmdbMediaSummary> getTvPopular(int page) {
        return getPage("/tv/popular", page, false);
    }

    public TmdbMovieDetailResponse getMovieDetail(long tmdbId) {
        return execute(() -> requireBody(restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("language", properties.getLanguage())
                        .queryParam("append_to_response", "credits,keywords")
                        .build(tmdbId))
                .retrieve()
                .body(TmdbMovieDetailResponse.class), "movie detail"));
    }

    public TmdbTvDetailResponse getTvDetail(long tmdbId) {
        return execute(() -> requireBody(restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{id}")
                        .queryParam("language", properties.getLanguage())
                        .queryParam("append_to_response", "credits,keywords,external_ids")
                        .build(tmdbId))
                .retrieve()
                .body(TmdbTvDetailResponse.class), "tv detail"));
    }

    public String toImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        return properties.getImageBaseUrl() + imagePath;
    }

    private TmdbPageResponse<TmdbMediaSummary> getPage(String path, int page, boolean includeRegion) {
        return execute(() -> requirePage(restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path(path)
                            .queryParam("language", properties.getLanguage())
                            .queryParam("page", page);
                    if (includeRegion) {
                        builder.queryParam("region", properties.getRegion());
                    }
                    return builder.build();
                })
                .retrieve()
                .body(PAGE_TYPE)));
    }

    private <T> T execute(java.util.function.Supplier<T> request) {
        return retryTemplate.invoke(request);
    }

    private <T> T requireBody(T body, String responseName) {
        if (body == null) {
            throw new RestClientException("TMDB " + responseName + " 응답 본문이 없습니다.");
        }
        return body;
    }

    private TmdbPageResponse<TmdbMediaSummary> requirePage(
            TmdbPageResponse<TmdbMediaSummary> response
    ) {
        requireBody(response, "page");
        if (response.results() == null) {
            throw new RestClientException("TMDB page 응답에 results가 없습니다.");
        }
        return response;
    }
}
