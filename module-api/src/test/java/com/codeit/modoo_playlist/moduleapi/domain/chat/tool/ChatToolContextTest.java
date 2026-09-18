package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

class ChatToolContextTest {

  @Test
  void userId가_있으면_UUID로_반환한다() {
    UUID userId = UUID.randomUUID();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.USER_ID, userId));

    assertThat(ChatToolContext.requireUserId(toolContext)).isEqualTo(userId);
  }

  @Test
  void userId가_없으면_예외를_던진다() {
    ToolContext toolContext = new ToolContext(Map.of());

    assertThatThrownBy(() -> ChatToolContext.requireUserId(toolContext))
        .isInstanceOf(IllegalStateException.class);
  }
}
