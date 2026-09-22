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

  @Test
  void parseUuid는_올바른_UUID_문자열을_변환한다() {
    UUID id = UUID.randomUUID();

    assertThat(ChatToolContext.parseUuid(id.toString())).isEqualTo(id);
  }

  @Test
  void parseUuid는_null이나_잘못된_형식이면_null을_반환한다() {
    assertThat(ChatToolContext.parseUuid(null)).isNull();
    assertThat(ChatToolContext.parseUuid("uuid-아님")).isNull();
  }
}
