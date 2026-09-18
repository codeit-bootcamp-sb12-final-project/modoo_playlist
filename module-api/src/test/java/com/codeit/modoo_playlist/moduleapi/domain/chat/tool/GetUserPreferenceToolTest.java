package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.service.UserPreferenceTagService;

@ExtendWith(MockitoExtension.class)
class GetUserPreferenceToolTest {

  @Mock private UserPreferenceTagService userPreferenceTagService;

  @Test
  void 로그인_사용자의_취향_태그를_반환한다() {
    UUID userId = UUID.randomUUID();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.USER_ID, userId));
    when(userPreferenceTagService.getMyPreferenceTags(userId, 5)).thenReturn(
        List.of(new UserPreferenceTagDto(UUID.randomUUID(), "액션", TagKind.GENRE, new BigDecimal("1.0"))));

    List<UserPreferenceTagDto> result =
        new GetUserPreferenceTool(userPreferenceTagService).getUserPreference(toolContext);

    assertThat(result).hasSize(1);
  }

  @Test
  void userId가_없으면_예외가_그대로_전파된다() {
    ToolContext toolContext = new ToolContext(Map.of());

    assertThatThrownBy(() -> new GetUserPreferenceTool(userPreferenceTagService).getUserPreference(toolContext))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void 조회_중_예외가_나면_빈_리스트를_반환한다() {
    UUID userId = UUID.randomUUID();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.USER_ID, userId));
    when(userPreferenceTagService.getMyPreferenceTags(userId, 5))
        .thenThrow(new RuntimeException("DB 장애"));

    List<UserPreferenceTagDto> result =
        new GetUserPreferenceTool(userPreferenceTagService).getUserPreference(toolContext);

    assertThat(result).isEmpty();
  }
}
