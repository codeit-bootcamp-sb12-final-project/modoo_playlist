package com.codeit.modoo_playlist.moduleapi.domain.search.event;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.codeit.modoo_playlist.moduleapi.domain.search.service.ContentIndexService;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.codeit.modoo_playlist.infra.event.kafka.WatcherCountChangedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatcherCountChangedEventListener {

  private static final int MAX_ATTEMPTS = 3;
  private static final long INITIAL_BACKOFF_MILLIS = 500L;

  private final ContentIndexService contentIndexService;

//  @Async("searchAsyncExecutor")
//  @EventListener
  @KafkaListener( // realtimemodule에서 신호를 받아서 수행해야함.
          topics = WatcherCountChangedEvent.TOPIC,
          groupId = "api-watcher-count-index"
  )
  public void handle(WatcherCountChangedEvent event) {
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        contentIndexService.updateWatcherCount(event.contentId());
        return;
      } catch (RuntimeException exception) {
        if (!isRetryable(exception) || attempt == MAX_ATTEMPTS) {
          log.error(
              "ES 시청자 수 갱신 최종 실패: contentId={}, attempts={}", event.contentId(), attempt, exception);
          return;
        }

        long backoffMillis =
            INITIAL_BACKOFF_MILLIS * (1L << (attempt - 1))
                + ThreadLocalRandom.current().nextLong(250L);

        log.warn(
            "ES 시청자 수 갱신 재시도 예정: " + "contentId={}, failedAttempt={}, delayMillis={}",
            event.contentId(), attempt, backoffMillis, exception);

        try {
          Thread.sleep(backoffMillis);
        } catch (InterruptedException interruptedException) {
          Thread.currentThread().interrupt();

          log.warn("ES 시청자 수 갱신 재시도 중단: contentId={}", event.contentId(), interruptedException);

          return;
        }
      }
    }
  }

  private boolean isRetryable(RuntimeException exception) {
    if (exception instanceof ElasticsearchException esException) {
      return switch (esException.status()) {
        case 409, 429, 502, 503, 504 -> true;
        default -> false;
      };
    }

    return exception instanceof IllegalStateException
        && exception.getCause() instanceof IOException;
  }

}
