package com.codeit.modoo_playlist.modulebatch.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

class ErrorCodeSkipPolicyTest {

    private final ErrorCodeSkipPolicy policy = new ErrorCodeSkipPolicy(
            Set.of(ErrorCode.TMDB_CONTENT_INVALID), 2
    );

    @Test
    void 허용된_에러코드는_limit_미만에서_skip한다() {
        assertThat(policy.shouldSkip(new BaseException(ErrorCode.TMDB_CONTENT_INVALID), 1)).isTrue();
    }

    @Test
    void 원인체인_안의_BaseException도_찾는다() {
        Throwable wrapped = new IllegalStateException(new BaseException(ErrorCode.TMDB_CONTENT_INVALID));
        assertThat(policy.shouldSkip(wrapped, 0)).isTrue();
    }

    @Test
    void 허용되지_않은_에러와_일반예외는_skip하지_않는다() {
        assertThat(policy.shouldSkip(new BaseException(ErrorCode.TMDB_AUTHENTICATION_FAILED), 0)).isFalse();
        assertThat(policy.shouldSkip(new IllegalStateException(), 0)).isFalse();
    }

    @Test
    void skipLimit에_도달하면_예외가_발생한다() {
        assertThatThrownBy(() -> policy.shouldSkip(
                new BaseException(ErrorCode.TMDB_CONTENT_INVALID), 2
        )).isInstanceOf(SkipLimitExceededException.class);
    }
}
