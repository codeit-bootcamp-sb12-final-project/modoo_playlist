package com.codeit.modoo_playlist.modulebatch.tmdb.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

@Component
public class TmdbStepFailureListener implements StepExecutionListener {

    private static final String FATAL_EXIT = "TMDB_FATAL";

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        boolean fatal = stepExecution.getFailureExceptions().stream().anyMatch(this::containsFatalCause);
        return fatal ? new ExitStatus(FATAL_EXIT) : null;
    }

    private boolean containsFatalCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BaseException baseException
                    && baseException.getErrorCode() == ErrorCode.TMDB_AUTHENTICATION_FAILED) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
