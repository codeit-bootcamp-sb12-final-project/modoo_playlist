package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
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
    assertThat(tool().searchContents(null, null, emptyContext)).isEmpty();
    assertThat(tool().searchContents("   ", null, emptyContext)).isEmpty();
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

    List<ContentDetailDto> result = tool().searchContents("우울할 때 볼 영화", null, toolContext);

    assertThat(result).containsExactly(detail);
  }

  @Test
  void 검색결과가_없으면_빈_리스트를_반환한다() {
    when(semanticSearchService.search("질의", 5)).thenReturn(List.of());
    when(contentDetailResolver.resolve(List.of(), emptyContext)).thenReturn(List.of());

    assertThat(tool().searchContents("질의", null, emptyContext)).isEmpty();
    verify(contentDetailResolver).resolve(List.of(), emptyContext);
  }

  @Test
  void 재검색이_아니면_여러_번_검색해도_카드를_유지하고_횟수를_제한하지_않는다() {
    ContentCardCollector collector = new ContentCardCollector();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, collector));
    collector.add(UUID.randomUUID(), "일본 스릴러 작품", null);
    when(semanticSearchService.search("한국 코미디", 5)).thenReturn(List.of());

    tool().searchContents("한국 코미디", null, toolContext);
    tool().searchContents("한국 코미디", false, toolContext);
    tool().searchContents("한국 코미디", null, toolContext);

    verify(semanticSearchService, times(3)).search("한국 코미디", 5);
    assertThat(collector.getCards()).extracting(ContentCardDto::title).containsExactly("일본 스릴러 작품");
  }

  @Test
  void 재검색하면_이전_검색의_카드를_비운다() {
    ContentCardCollector collector = new ContentCardCollector();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, collector));
    collector.add(UUID.randomUUID(), "첫 검색 작품", null);
    when(semanticSearchService.search("2010년대 일본 스릴러", 5)).thenReturn(List.of());

    tool().searchContents("2010년대 일본 스릴러", true, toolContext);

    assertThat(collector.getCards()).isEmpty();
  }

  @Test
  void 재검색_한도를_넘으면_검색하지_않고_직전_카드를_유지한다() {
    ContentCardCollector collector = new ContentCardCollector();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, collector));
    collector.nextRetry();
    collector.add(UUID.randomUUID(), "재검색 작품", null);

    assertThat(tool().searchContents("한 번 더", true, toolContext)).isEmpty();

    verifyNoInteractions(semanticSearchService, contentDetailResolver);
    assertThat(collector.getCards()).extracting(ContentCardDto::title).containsExactly("재검색 작품");
  }

  @Test
  void 검색_중_예외가_나면_삼키지_않고_전파한다() {
    when(semanticSearchService.search("질의", 5)).thenThrow(new RuntimeException("검색 엔진 장애"));

    assertThatThrownBy(() -> tool().searchContents("질의", null, emptyContext))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("검색 엔진 장애");
    verifyNoInteractions(contentDetailResolver);
  }
}
