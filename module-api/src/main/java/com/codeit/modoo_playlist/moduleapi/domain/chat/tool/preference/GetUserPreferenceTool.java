package com.codeit.modoo_playlist.moduleapi.domain.chat.tool.preference;

import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.ChatToolContext;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.service.UserPreferenceTagService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetUserPreferenceTool {
  private static final int DEFAULT_LIMIT = 5;

  private final UserPreferenceTagService userPreferenceTagService;

  @Tool(
      name = "get_user_preference",
      description = "로그인한 사용자가 선호하는 태그(장르 등)와 점수를 조회합니다."
  )
  public List<UserPreferenceTagDto> getUserPreference(ToolContext toolContext) {
    UUID userId = ChatToolContext.requireUserId(toolContext);
    log.info("get_user_preference 호출: userId={}", userId);
    List<UserPreferenceTagDto> result = userPreferenceTagService.getMyPreferenceTags(userId, DEFAULT_LIMIT);
    log.info("get_user_preference 결과: {}건", result.size());
    return result;
  }
}
