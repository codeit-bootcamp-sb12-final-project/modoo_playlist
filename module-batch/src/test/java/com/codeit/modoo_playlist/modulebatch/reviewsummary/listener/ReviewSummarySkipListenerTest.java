package com.codeit.modoo_playlist.modulebatch.reviewsummary.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class ReviewSummarySkipListenerTest {

  private final ReviewSummarySkipListener listener = new ReviewSummarySkipListener();
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @BeforeEach
  void setUp() {
    appender.start();
    logger().addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    logger().detachAppender(appender);
  }

  @Test
  void BaseException을_직접_받으면_에러코드를_로그에_남긴다() {
    listener.onSkipInProcess(target(), new BaseException(ErrorCode.REVIEW_SUMMARY_GENERATION_FAILED));

    assertThat(loggedMessage())
        .contains("contentId=c1")
        .contains("title=제목")
        .contains("code=REVIEW_SUMMARY_GENERATION_FAILED");
  }

  @Test
  void 원인체인_안의_BaseException도_찾아_에러코드를_로그에_남긴다() {
    Throwable wrapped = new RuntimeException("wrapper",
        new BaseException(ErrorCode.REVIEW_SUMMARY_GENERATION_FAILED));

    listener.onSkipInProcess(target(), wrapped);

    assertThat(loggedMessage()).contains("code=REVIEW_SUMMARY_GENERATION_FAILED");
  }

  @Test
  void BaseException이_아니면_code는_null로_남기고_메시지만_기록한다() {
    listener.onSkipInProcess(target(), new IllegalStateException("알수없는오류"));

    assertThat(loggedMessage())
        .contains("code=null")
        .contains("reason=알수없는오류");
  }

  private String loggedMessage() {
    List<ILoggingEvent> events = appender.list;
    assertThat(events).hasSize(1);
    return events.get(0).getFormattedMessage();
  }

  private ReviewSummaryTarget target() {
    return new ReviewSummaryTarget("c1", "제목", List.of());
  }

  private Logger logger() {
    return (Logger) LoggerFactory.getLogger(ReviewSummarySkipListener.class);
  }
}
