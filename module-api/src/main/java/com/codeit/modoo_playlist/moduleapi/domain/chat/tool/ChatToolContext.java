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
      throw new IllegalStateException(
          "로그인 사용자 전용 툴인데 toolContext에 " + USER_ID + "가 없습니다. 비로그인 경로에 등록된 건 아닌지 확인하세요.");
    }
    return (UUID) userId;
  }
}
