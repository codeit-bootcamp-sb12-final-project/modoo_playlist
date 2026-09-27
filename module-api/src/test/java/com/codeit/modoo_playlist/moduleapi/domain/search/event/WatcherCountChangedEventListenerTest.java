package com.codeit.modoo_playlist.moduleapi.domain.search.event;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.codeit.modoo_playlist.infra.event.kafka.WatcherCountChangedEvent;
import com.codeit.modoo_playlist.moduleapi.domain.search.service.ContentIndexService;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatcherCountChangedEventListenerTest {

  private static final UUID CONTENT_ID = UUID.fromString("019ed8a0-0000-7000-9300-000000000001");

  @Mock
  private ContentIndexService contentIndexService;

  @InjectMocks
  private WatcherCountChangedEventListener listener;

  @Test
  @DisplayName("시청자 수 변경 이벤트를 받으면 검색 인덱스의 시청자 수를 갱신한다")
  void updateWatcherCount() {
    WatcherCountChangedEvent event = new WatcherCountChangedEvent(CONTENT_ID);

    listener.handle(event);

    verify(contentIndexService).updateWatcherCount(CONTENT_ID);
  }

  @Test
  @DisplayName("재시도할 수 없는 ES 오류가 발생하면 즉시 처리를 종료한다")
  void stopOnNonRetryableError() {
    WatcherCountChangedEvent event = new WatcherCountChangedEvent(CONTENT_ID);
    ElasticsearchException exception = mock(ElasticsearchException.class);

    when(exception.status()).thenReturn(400);
    doThrow(exception).when(contentIndexService).updateWatcherCount(CONTENT_ID);

    listener.handle(event);

    verify(contentIndexService).updateWatcherCount(CONTENT_ID);
  }

  @Test
  @DisplayName("재시도 가능한 ES 오류가 발생하면 다시 시도한다")
  void retryOnRetryableError() {
    WatcherCountChangedEvent event = new WatcherCountChangedEvent(CONTENT_ID);
    ElasticsearchException exception = mock(ElasticsearchException.class);

    when(exception.status()).thenReturn(503);
    doThrow(exception).doNothing().when(contentIndexService).updateWatcherCount(CONTENT_ID);

    listener.handle(event);

    verify(contentIndexService, times(2)).updateWatcherCount(CONTENT_ID);
  }

  @Test
  @DisplayName("재시도 가능한 ES 오류가 계속 발생하면 최대 재시도 횟수 후 중단한다")
  void stopAfterMaxRetries() {
    WatcherCountChangedEvent event = new WatcherCountChangedEvent(CONTENT_ID);
    ElasticsearchException exception = mock(ElasticsearchException.class);

    when(exception.status()).thenReturn(503);
    doThrow(exception).when(contentIndexService).updateWatcherCount(CONTENT_ID);

    listener.handle(event);

    verify(contentIndexService, times(3)).updateWatcherCount(CONTENT_ID);
  }

  @Test
  @DisplayName("IOException이 원인인 IllegalStateException이 발생하면 재시도한다")
  void retryOnIOException() {
    IllegalStateException exception =
        new IllegalStateException("ES 요청 실패", new IOException("연결 실패"));

    doThrow(exception).doNothing().when(contentIndexService).updateWatcherCount(CONTENT_ID);

    listener.handle(new WatcherCountChangedEvent(CONTENT_ID));

    verify(contentIndexService, times(2)).updateWatcherCount(CONTENT_ID);
  }
}
