package com.codeit.modoo_playlist.moduleapi.domain.chat.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.codeit.modoo_playlist.moduleapi.domain.chat.event.ChatMemoryClearRequestedEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;

@ExtendWith(MockitoExtension.class)
class ChatMemoryEventListenerTest {

  @Mock private ChatMemory chatMemory;

  private ChatMemoryEventListener listener() {
    return new ChatMemoryEventListener(chatMemory);
  }

  @Test
  void 이벤트를_받으면_해당_대화의_ChatMemory를_비운다() {
    UUID conversationId = UUID.randomUUID();

    listener().handleChatMemoryClearRequested(new ChatMemoryClearRequestedEvent(conversationId));

    verify(chatMemory).clear(conversationId.toString());
  }

  @Test
  void ChatMemory_정리가_실패해도_예외를_전파하지_않는다() {
    UUID conversationId = UUID.randomUUID();
    doThrow(new RuntimeException("Redis 장애")).when(chatMemory).clear(conversationId.toString());

    assertThatCode(() ->
        listener().handleChatMemoryClearRequested(new ChatMemoryClearRequestedEvent(conversationId)))
        .doesNotThrowAnyException();
  }
}
