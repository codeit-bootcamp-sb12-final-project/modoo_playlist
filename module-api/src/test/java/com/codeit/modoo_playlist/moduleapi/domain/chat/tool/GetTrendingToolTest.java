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
class GetTrendingToolTest {

  @Mock private RecommendationService recommendationService;
  @Mock private ContentDetailResolver contentDetailResolver;

  private final ToolContext toolContext = new ToolContext(Map.of());

  private GetTrendingTool tool() {
    return new GetTrendingTool(recommendationService, contentDetailResolver);
  }

  @Test
  void 인기_콘텐츠의_상세정보를_반환한다() {
    UUID contentId = UUID.randomUUID();
    List<RecommendedContentDto> trending = List.of(new RecommendedContentDto(contentId, "인기작", "thumb", 4.8));
    when(recommendationService.getTrendingContents(5)).thenReturn(trending);
    ContentDetailDto detail = ContentDetailDto.titleOnly(contentId, "인기작");
    when(contentDetailResolver.resolve(trending, toolContext)).thenReturn(List.of(detail));

    List<ContentDetailDto> result = tool().getTrending(toolContext);

    assertThat(result).containsExactly(detail);
  }

  @Test
  void 조회_중_예외가_나면_삼키지_않고_전파한다() {
    when(recommendationService.getTrendingContents(5)).thenThrow(new RuntimeException("DB 장애"));

    assertThatThrownBy(() -> tool().getTrending(toolContext)).hasMessage("DB 장애");
    verifyNoInteractions(contentDetailResolver);
  }
}
