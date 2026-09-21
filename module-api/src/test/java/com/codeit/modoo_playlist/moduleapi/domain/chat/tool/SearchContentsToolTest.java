package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.SemanticSearchService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
class SearchContentsToolTest {

  @Mock private SemanticSearchService semanticSearchService;
  @Mock private ContentDetailResolver contentDetailResolver;

  private final ToolContext emptyContext = new ToolContext(Map.of());

  private SearchContentsTool tool() {
    return new SearchContentsTool(semanticSearchService, contentDetailResolver);
  }

  @Test
  void query가_비어있으면_검색없이_빈_리스트를_반환한다() {
    assertThat(tool().searchContents(null, emptyContext)).isEmpty();
    assertThat(tool().searchContents("   ", emptyContext)).isEmpty();
    verifyNoInteractions(semanticSearchService, contentDetailResolver);
  }

  @Test
  void 정상_질의면_검색결과의_상세정보를_반환한다() {
    UUID contentId = UUID.randomUUID();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, new ContentCardCollector()));
    List<RecommendedContentDto> hits = List.of(new RecommendedContentDto(contentId, "제목", "thumb", 0.8));
    when(semanticSearchService.search("우울할 때 볼 영화", 5)).thenReturn(hits);
    ContentDetailDto detail = ContentDetailDto.titleOnly(contentId, "제목");
    when(contentDetailResolver.resolve(hits, toolContext)).thenReturn(List.of(detail));

    List<ContentDetailDto> result = tool().searchContents("우울할 때 볼 영화", toolContext);

    assertThat(result).containsExactly(detail);
  }

  @Test
  void 검색결과가_없으면_빈_리스트를_반환한다() {
    when(semanticSearchService.search("질의", 5)).thenReturn(List.of());
    when(contentDetailResolver.resolve(List.of(), emptyContext)).thenReturn(List.of());

    assertThat(tool().searchContents("질의", emptyContext)).isEmpty();
    verify(contentDetailResolver).resolve(List.of(), emptyContext);
  }

  @Test
  void 검색_중_예외가_나면_빈_리스트를_반환한다() {
    when(semanticSearchService.search("질의", 5)).thenThrow(new RuntimeException("검색 엔진 장애"));

    List<ContentDetailDto> result = tool().searchContents("질의", emptyContext);

    assertThat(result).isEmpty();
    verifyNoInteractions(contentDetailResolver);
  }
}
