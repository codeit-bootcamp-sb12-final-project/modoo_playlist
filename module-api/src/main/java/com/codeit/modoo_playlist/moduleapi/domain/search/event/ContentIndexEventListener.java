package com.codeit.modoo_playlist.moduleapi.domain.search.event;

import com.codeit.modoo_playlist.moduleapi.domain.search.service.ContentIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ContentIndexEventListener {

  private final ContentIndexService contentIndexService;

  @Async("searchAsyncExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ContentIndexRequestedEvent event) {
    contentIndexService.index(event.contentId());
  }

}
