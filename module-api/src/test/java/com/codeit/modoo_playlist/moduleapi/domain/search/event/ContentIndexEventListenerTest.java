package com.codeit.modoo_playlist.moduleapi.domain.search.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.codeit.modoo_playlist.infra.event.kafka.IndexKafkaEvent;
import com.codeit.modoo_playlist.infra.event.kafka.IndexKafkaPublisher;
import com.codeit.modoo_playlist.moduleapi.domain.search.service.ContentIndexService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class ContentIndexEventListenerTest {

  private static final UUID TEST_CONTENT_ID =
      UUID.fromString("019ed8a0-0000-7000-8000-000000000002");

  @Mock
  private ContentIndexService contentIndexService;

  @Mock
  private KafkaTemplate<String, IndexKafkaEvent> kafkaTemplate;

  @InjectMocks
  private ContentIndexEventListener contentIndexEventListener;

  @InjectMocks
  private IndexKafkaListener indexKafkaListener;

  private IndexKafkaPublisher indexKafkaPublisher;

  @BeforeEach
  void setUp() {
    indexKafkaPublisher = new IndexKafkaPublisher(kafkaTemplate);
  }

  @Test
  @DisplayName("색인 요청 이벤트를 받으면 콘텐츠를 색인한다")
  void handleContentIndexRequestedEvent() {
    ContentIndexRequestedEvent event = new ContentIndexRequestedEvent(TEST_CONTENT_ID);

    contentIndexEventListener.handle(event);

    verify(contentIndexService).index(TEST_CONTENT_ID);
  }

  @Test
  @DisplayName("Kafka 색인 이벤트를 받으면 콘텐츠를 색인한다")
  void handleIndexKafkaEvent() {
    IndexKafkaEvent event = new IndexKafkaEvent(TEST_CONTENT_ID);

    indexKafkaListener.handle(event);

    verify(contentIndexService).index(TEST_CONTENT_ID);
  }

  @Test
  @DisplayName("콘텐츠 ID를 포함한 색인 이벤트를 Kafka로 전송한다")
  void publishIndexKafkaEvent() {
    CompletableFuture<SendResult<String, IndexKafkaEvent>> future =
        CompletableFuture.completedFuture(null);

    when(kafkaTemplate.send(
        eq(IndexKafkaPublisher.TOPIC),
        eq(TEST_CONTENT_ID.toString()),
        any(IndexKafkaEvent.class)
    )).thenReturn(future);

    indexKafkaPublisher.publish(TEST_CONTENT_ID);

    ArgumentCaptor<IndexKafkaEvent> eventCaptor = ArgumentCaptor.forClass(IndexKafkaEvent.class);

    verify(kafkaTemplate).send(
        eq(IndexKafkaPublisher.TOPIC),
        eq(TEST_CONTENT_ID.toString()),
        eventCaptor.capture()
    );

    assertThat(eventCaptor.getValue().contentId()).isEqualTo(TEST_CONTENT_ID);
  }

  @Test
  @DisplayName("Kafka 전송 실패 시 오류 로그를 기록한다")
  void handleIndexKafkaPublishFailure() {
    CompletableFuture<SendResult<String, IndexKafkaEvent>> future = new CompletableFuture<>();

    when(kafkaTemplate.send(
        eq(IndexKafkaPublisher.TOPIC),
        eq(TEST_CONTENT_ID.toString()),
        any(IndexKafkaEvent.class)
    )).thenReturn(future);

    Logger logger = (Logger) LoggerFactory.getLogger(IndexKafkaPublisher.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    try {
      indexKafkaPublisher.publish(TEST_CONTENT_ID);

      future.completeExceptionally(new RuntimeException("Kafka 전송 실패"));

      assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage())
                .contains("콘텐츠 검색 색인 Kafka 이벤트 전송 실패")
                .contains(TEST_CONTENT_ID.toString());
          });
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }
}
