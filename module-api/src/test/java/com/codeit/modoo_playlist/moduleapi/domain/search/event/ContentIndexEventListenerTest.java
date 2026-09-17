package com.codeit.modoo_playlist.moduleapi.domain.search.event;

import static org.mockito.Mockito.verify;

import com.codeit.modoo_playlist.moduleapi.domain.search.service.ContentIndexService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentIndexEventListenerTest {

  private static final UUID TEST_CONTENT_ID =
      UUID.fromString("019ed8a0-0000-7000-8000-000000000002");

  @Mock
  private ContentIndexService contentIndexService;

  @InjectMocks
  private ContentIndexEventListener contentIndexEventListener;

  @Test
  @DisplayName("색인 요청 이벤트를 받으면 콘텐츠를 색인한다")
  void handle() {
    ContentIndexRequestedEvent event =
        new ContentIndexRequestedEvent(TEST_CONTENT_ID);

    contentIndexEventListener.handle(event);

    verify(contentIndexService).index(TEST_CONTENT_ID);
  }
}
