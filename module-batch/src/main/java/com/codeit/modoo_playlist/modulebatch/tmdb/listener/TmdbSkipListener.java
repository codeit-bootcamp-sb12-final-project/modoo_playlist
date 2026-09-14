package com.codeit.modoo_playlist.modulebatch.tmdb.listener;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbFetchedContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbSyncContent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TmdbSkipListener implements SkipListener<TmdbFetchedContent, TmdbSyncContent> {

    @Override
    public void onSkipInProcess(TmdbFetchedContent fetched, Throwable throwable) {
        BaseException baseException = findBaseException(throwable);
        log.warn(
                "TMDB 콘텐츠 동기화를 건너뜁니다. mediaType={}, tmdbId={}, code={}, details={}, reason={}",
                fetched.candidate().mediaType(),
                fetched.candidate().tmdbId(),
                baseException == null ? null : baseException.getErrorCode(),
                baseException == null ? null : baseException.getDetails(),
                throwable.getMessage()
        );
    }

    private BaseException findBaseException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BaseException baseException) {
                return baseException;
            }
            current = current.getCause();
        }
        return null;
    }
}
