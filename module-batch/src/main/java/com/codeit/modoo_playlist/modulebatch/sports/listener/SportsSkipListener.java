package com.codeit.modoo_playlist.modulebatch.sports.listener;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.infra.client.sportsdb.dto.SportsDbEvent;
import com.codeit.modoo_playlist.modulebatch.sports.model.SportsSyncContent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SportsSkipListener implements SkipListener<SportsDbEvent, SportsSyncContent> {

    @Override
    public void onSkipInProcess(SportsDbEvent event, Throwable throwable) {
        BaseException baseException = findBaseException(throwable);
        log.warn(
                "TheSportsDB 경기 동기화를 건너뜁니다. idEvent={}, code={}, details={}, reason={}",
                event == null ? null : event.idEvent(),
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
