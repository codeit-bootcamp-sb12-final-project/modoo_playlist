package com.codeit.modoo_playlist.modulebatch.support;

import java.util.Set;

import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

public class ErrorCodeSkipPolicy implements SkipPolicy {

    private final Set<ErrorCode> skippableCodes;
    private final long skipLimit;

    public ErrorCodeSkipPolicy(Set<ErrorCode> skippableCodes, long skipLimit) {
        this.skippableCodes = Set.copyOf(skippableCodes);
        this.skipLimit = skipLimit;
    }

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) {
        BaseException baseException = findBaseException(throwable);
        if (baseException == null || !skippableCodes.contains(baseException.getErrorCode())) {
            return false;
        }
        if (skipCount >= skipLimit) {
            throw new SkipLimitExceededException(skipLimit, throwable);
        }
        return true;
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
