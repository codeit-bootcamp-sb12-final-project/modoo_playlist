package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import java.util.UUID;
import org.springframework.ai.chat.model.ToolContext;

public final class ChatToolContext {

  public static final String USER_ID = "userId";
  public static final String CARD_COLLECTOR = "cardCollector";

  private ChatToolContext() {
  }

  public static UUID requireUserId(ToolContext toolContext) {
    Object userId = toolContext.getContext().get(USER_ID);
    if (userId == null) {
      throw new IllegalStateException("toolContext에 " + USER_ID + "가 없습니다.");
    }
    return (UUID) userId;
  }

  public static UUID parseUuid(String value) {
    if (value == null) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
