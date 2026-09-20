package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
class RecommendContentsToolTest {

  @Mock private RecommendationService recommendationService;

  private final ToolContext emptyContext = new ToolContext(Map.of());

  @Test
  void contentId가_없으면_빈_리스트를_반환한다() {
    List<RecommendedContentDto> result =
        new RecommendContentsTool(recommendationService).recommendContents(null, emptyContext);

    assertThat(result).isEmpty();
    verifyNoInteractions(recommendationService);
  }

  @Test
  void contentId_형식이_올바르지_않으면_빈_리스트를_반환한다() {
    List<RecommendedContentDto> result =
        new RecommendContentsTool(recommendationService).recommendContents("uuid-아님", emptyContext);

    assertThat(result).isEmpty();
    verifyNoInteractions(recommendationService);
  }

  @Test
  void 정상_UUID면_유사_콘텐츠를_반환하고_카드_컬렉터에_담는다() {
    UUID contentId = UUID.randomUUID();
    UUID similarId = UUID.randomUUID();
    ContentCardCollector collector = new ContentCardCollector();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, collector));
    when(recommendationService.getSimilarContents(contentId, 5)).thenReturn(
        List.of(new RecommendedContentDto(similarId, "비슷한 콘텐츠", "thumb", 0.9)));

    List<RecommendedContentDto> result = new RecommendContentsTool(recommendationService)
        .recommendContents(contentId.toString(), toolContext);

    assertThat(result).extracting(RecommendedContentDto::contentId).containsExactly(similarId);
    assertThat(collector.getCards()).hasSize(1);
  }

  @Test
  void 조회_중_예외가_나면_빈_리스트를_반환한다() {
    UUID contentId = UUID.randomUUID();
    when(recommendationService.getSimilarContents(contentId, 5))
        .thenThrow(new RuntimeException("DB 장애"));

    List<RecommendedContentDto> result = new RecommendContentsTool(recommendationService)
        .recommendContents(contentId.toString(), emptyContext);

    assertThat(result).isEmpty();
  }
}
