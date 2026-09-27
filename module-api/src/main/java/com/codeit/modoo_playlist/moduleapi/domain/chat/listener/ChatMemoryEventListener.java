package com.codeit.modoo_playlist.moduleapi.domain.chat.listener;

import com.codeit.modoo_playlist.moduleapi.domain.chat.event.ChatMemoryClearRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChatMemoryEventListener {

  private final ChatMemory chatMemory;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleChatMemoryClearRequested(ChatMemoryClearRequestedEvent event) {
    try {
      chatMemory.clear(event.conversationId().toString());
    } catch (Exception e) {
      log.error("대화 삭제 후 ChatMemory 정리 실패: conversationId={}", event.conversationId(), e);
    }
  }
}
