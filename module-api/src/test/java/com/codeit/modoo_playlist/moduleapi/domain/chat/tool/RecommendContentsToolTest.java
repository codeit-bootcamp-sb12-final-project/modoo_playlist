package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
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
class RecommendContentsToolTest {

  @Mock private RecommendationService recommendationService;
  @Mock private ContentDetailResolver contentDetailResolver;

  private final ToolContext emptyContext = new ToolContext(Map.of());

  private RecommendContentsTool tool() {
    return new RecommendContentsTool(recommendationService, contentDetailResolver);
  }

  @Test
  void contentId가_없으면_빈_리스트를_반환한다() {
    List<ContentDetailDto> result = tool().recommendContents(null, emptyContext);

    assertThat(result).isEmpty();
    verifyNoInteractions(recommendationService, contentDetailResolver);
  }

  @Test
  void contentId_형식이_올바르지_않으면_빈_리스트를_반환한다() {
    List<ContentDetailDto> result = tool().recommendContents("uuid-아님", emptyContext);

    assertThat(result).isEmpty();
    verifyNoInteractions(recommendationService, contentDetailResolver);
  }

  @Test
  void 정상_UUID면_유사_콘텐츠의_상세정보를_반환한다() {
    UUID contentId = UUID.randomUUID();
    UUID similarId = UUID.randomUUID();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, new ContentCardCollector()));
    List<RecommendedContentDto> similar = List.of(new RecommendedContentDto(similarId, "비슷한 콘텐츠", "thumb", 0.9));
    when(recommendationService.getSimilarContents(contentId, 5)).thenReturn(similar);
    ContentDetailDto detail = ContentDetailDto.titleOnly(similarId, "비슷한 콘텐츠");
    when(contentDetailResolver.resolve(similar, toolContext)).thenReturn(List.of(detail));

    List<ContentDetailDto> result = tool().recommendContents(contentId.toString(), toolContext);

    assertThat(result).containsExactly(detail);
  }

  @Test
  void 조회_중_예외가_나면_빈_리스트를_반환한다() {
    UUID contentId = UUID.randomUUID();
    when(recommendationService.getSimilarContents(contentId, 5))
        .thenThrow(new RuntimeException("DB 장애"));

    List<ContentDetailDto> result = tool().recommendContents(contentId.toString(), emptyContext);

    assertThat(result).isEmpty();
    verifyNoInteractions(contentDetailResolver);
  }
}
