package com.codeit.modoo_playlist.modulebatch.tmdb.listener;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbFetchedContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbSyncContent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TmdbSkipListener implements SkipListener<TmdbFetchedContent, TmdbSyncContent> {

    @Override
    public void onSkipInProcess(TmdbFetchedContent fetched, Throwable throwable) {
        log.warn(
                "TMDB 콘텐츠 동기화를 건너뜁니다. mediaType={}, tmdbId={}, reason={}",
                fetched.candidate().mediaType(),
                fetched.candidate().tmdbId(),
                throwable.getMessage()
        );
    }
}
