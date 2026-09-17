package com.codeit.modoo_playlist.modulebatch.reviewsummary.listener;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryResult;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReviewSummarySkipListener implements SkipListener<ReviewSummaryTarget, ReviewSummaryResult> {

  @Override
  public void onSkipInProcess(ReviewSummaryTarget target, Throwable throwable) {
    BaseException baseException = findBaseException(throwable);
    log.warn(
        "리뷰 요약 생성을 건너뜁니다. contentId={}, title={}, code={}, details={}, reason={}",
        target.contentId(),
        target.title(),
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
