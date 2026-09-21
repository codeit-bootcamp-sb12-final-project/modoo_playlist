package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.RecommendationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
class GetPersonalizedRecommendationsToolTest {

  @Mock private RecommendationService recommendationService;
  @Mock private ContentDetailResolver contentDetailResolver;

  private GetPersonalizedRecommendationsTool tool() {
    return new GetPersonalizedRecommendationsTool(recommendationService, contentDetailResolver);
  }

  @Test
  void 로그인_사용자의_개인화_추천_상세정보를_반환한다() {
    UUID userId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ToolContext toolContext = new ToolContext(Map.of(
        ChatToolContext.USER_ID, userId, ChatToolContext.CARD_COLLECTOR, new ContentCardCollector()));
    List<RecommendedContentDto> recommended = List.of(new RecommendedContentDto(contentId, "제목", "thumb", 1.0));
    when(recommendationService.getRecommendationsForMe(userId, 5)).thenReturn(recommended);
    ContentDetailDto detail = ContentDetailDto.titleOnly(contentId, "제목");
    when(contentDetailResolver.resolve(recommended, toolContext)).thenReturn(List.of(detail));

    List<ContentDetailDto> result = tool().getPersonalizedRecommendations(toolContext);

    assertThat(result).containsExactly(detail);
  }

  @Test
  void userId가_없으면_예외가_그대로_전파된다() {
    ToolContext toolContext = new ToolContext(Map.of());

    assertThatThrownBy(() -> tool().getPersonalizedRecommendations(toolContext))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(contentDetailResolver);
  }

  @Test
  void 조회_중_예외가_나면_빈_리스트를_반환하고_예외를_삼킨다() {
    UUID userId = UUID.randomUUID();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.USER_ID, userId));
    when(recommendationService.getRecommendationsForMe(userId, 5))
        .thenThrow(new RuntimeException("DB 장애"));

    List<ContentDetailDto> result = tool().getPersonalizedRecommendations(toolContext);

    assertThat(result).isEmpty();
    verifyNoInteractions(contentDetailResolver);
  }

  @Test
  void 카드_컬렉터가_context에_없으면_그냥_결과만_반환한다() {
    UUID userId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.USER_ID, userId));
    List<RecommendedContentDto> recommended = List.of(new RecommendedContentDto(contentId, "제목", null, 1.0));
    when(recommendationService.getRecommendationsForMe(userId, 5)).thenReturn(recommended);
    when(contentDetailResolver.resolve(recommended, toolContext))
        .thenReturn(List.of(ContentDetailDto.titleOnly(contentId, "제목")));

    List<ContentDetailDto> result = tool().getPersonalizedRecommendations(toolContext);

    assertThat(result).hasSize(1);
  }
}
