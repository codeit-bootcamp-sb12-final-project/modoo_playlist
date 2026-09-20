package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
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

  @Mock
  private RecommendationService recommendationService;

  @Test
  void 로그인_사용자의_개인화_추천을_반환하고_카드_컬렉터에_담는다() {
    UUID userId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    ContentCardCollector collector = new ContentCardCollector();
    ToolContext toolContext = new ToolContext(Map.of(
        ChatToolContext.USER_ID, userId, ChatToolContext.CARD_COLLECTOR, collector));
    when(recommendationService.getRecommendationsForMe(userId, 5)).thenReturn(
        List.of(new RecommendedContentDto(contentId, "제목", "thumb", 1.0)));

    List<RecommendedContentDto> result =
        new GetPersonalizedRecommendationsTool(
            recommendationService).getPersonalizedRecommendations(toolContext);

    assertThat(result).extracting(RecommendedContentDto::contentId).containsExactly(contentId);
    assertThat(collector.getCards())
        .containsExactly(new ContentCardDto(contentId, "제목", "thumb"));
  }

  @Test
  void userId가_없으면_예외가_그대로_전파된다() {
    ToolContext toolContext = new ToolContext(Map.of());

    assertThatThrownBy(() ->
        new GetPersonalizedRecommendationsTool(
            recommendationService).getPersonalizedRecommendations(toolContext)
    ).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void 조회_중_예외가_나면_빈_리스트를_반환하고_예외를_삼킨다() {
    UUID userId = UUID.randomUUID();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.USER_ID, userId));
    when(recommendationService.getRecommendationsForMe(userId, 5))
        .thenThrow(new RuntimeException("DB 장애"));

    List<RecommendedContentDto> result =
        new GetPersonalizedRecommendationsTool(
            recommendationService).getPersonalizedRecommendations(toolContext);

    assertThat(result).isEmpty();
  }

  @Test
  void 카드_컬렉터가_context에_없으면_그냥_결과만_반환한다() {
    UUID userId = UUID.randomUUID();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.USER_ID, userId));
    when(recommendationService.getRecommendationsForMe(userId, 5)).thenReturn(
        List.of(new RecommendedContentDto(UUID.randomUUID(), "제목", null, 1.0)));

    List<RecommendedContentDto> result =
        new GetPersonalizedRecommendationsTool(
            recommendationService).getPersonalizedRecommendations(toolContext);

    assertThat(result).hasSize(1);
  }
}
