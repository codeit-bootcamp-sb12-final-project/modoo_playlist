package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.SemanticSearchService;

@ExtendWith(MockitoExtension.class)
class SearchContentsToolTest {

  @Mock private SemanticSearchService semanticSearchService;

  private final ToolContext emptyContext = new ToolContext(Map.of());

  @Test
  void query가_비어있으면_검색없이_빈_리스트를_반환한다() {
    assertThat(new SearchContentsTool(semanticSearchService).searchContents(null, emptyContext)).isEmpty();
    assertThat(new SearchContentsTool(semanticSearchService).searchContents("   ", emptyContext)).isEmpty();
  }

  @Test
  void 정상_질의면_검색결과를_반환하고_카드_컬렉터에_담는다() {
    UUID contentId = UUID.randomUUID();
    ContentCardCollector collector = new ContentCardCollector();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, collector));
    when(semanticSearchService.search("우울할 때 볼 영화", 5)).thenReturn(
        List.of(new RecommendedContentDto(contentId, "제목", "thumb", 0.8)));

    List<RecommendedContentDto> result =
        new SearchContentsTool(semanticSearchService).searchContents("우울할 때 볼 영화", toolContext);

    assertThat(result).extracting(RecommendedContentDto::contentId).containsExactly(contentId);
    assertThat(collector.getCards()).hasSize(1);
  }

  @Test
  void 검색_중_예외가_나면_빈_리스트를_반환한다() {
    when(semanticSearchService.search("질의", 5)).thenThrow(new RuntimeException("검색 엔진 장애"));

    List<RecommendedContentDto> result =
        new SearchContentsTool(semanticSearchService).searchContents("질의", emptyContext);

    assertThat(result).isEmpty();
  }
}
