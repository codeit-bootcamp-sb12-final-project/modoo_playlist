package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.ContentDetailService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
class ContentDetailResolverTest {

  @Mock private ContentDetailService contentDetailService;

  private final ToolContext emptyContext = new ToolContext(Map.of());

  private ContentDetailResolver resolver() {
    return new ContentDetailResolver(contentDetailService);
  }

  @Test
  void 검색결과의_ID로_상세정보를_조회해_반환한다() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    List<ContentDetailDto> details = List.of(
        ContentDetailDto.titleOnly(first, "첫째"), ContentDetailDto.titleOnly(second, "둘째"));
    when(contentDetailService.getDetails(List.of(first, second))).thenReturn(details);

    List<ContentDetailDto> result = resolver().resolve(List.of(
        new RecommendedContentDto(first, "첫째", "thumb1", 0.9),
        new RecommendedContentDto(second, "둘째", "thumb2", 0.8)), emptyContext);

    assertThat(result).isEqualTo(details);
  }

  @Test
  void 상세_조회가_실패하면_제목만_담아_순서대로_반환한다() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    when(contentDetailService.getDetails(List.of(first, second))).thenThrow(new RuntimeException("DB 장애"));

    List<ContentDetailDto> result = resolver().resolve(List.of(
        new RecommendedContentDto(first, "첫째", "thumb1", 0.9),
        new RecommendedContentDto(second, "둘째", "thumb2", 0.8)), emptyContext);

    assertThat(result).containsExactly(
        ContentDetailDto.titleOnly(first, "첫째"), ContentDetailDto.titleOnly(second, "둘째"));
  }

  @Test
  void 결과가_없으면_빈_리스트를_반환한다() {
    when(contentDetailService.getDetails(List.of())).thenReturn(List.of());

    assertThat(resolver().resolve(List.of(), emptyContext)).isEmpty();
  }

  @Test
  void 상세_결과에_남은_콘텐츠만_카드로_수집한다() {
    UUID alive = UUID.randomUUID();
    UUID deleted = UUID.randomUUID();
    ContentCardCollector collector = new ContentCardCollector();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, collector));
    when(contentDetailService.getDetails(List.of(alive, deleted)))
        .thenReturn(List.of(ContentDetailDto.titleOnly(alive, "살아있음")));

    resolver().resolve(List.of(
        new RecommendedContentDto(alive, "살아있음", "thumb1", 0.9),
        new RecommendedContentDto(deleted, "삭제됨", "thumb2", 0.8)), toolContext);

    assertThat(collector.getCards()).containsExactly(new ContentCardDto(alive, "살아있음", "thumb1"));
  }

  @Test
  void 상세_조회가_실패해도_검색결과는_모두_카드로_수집한다() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    ContentCardCollector collector = new ContentCardCollector();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, collector));
    when(contentDetailService.getDetails(List.of(first, second))).thenThrow(new RuntimeException("DB 장애"));

    resolver().resolve(List.of(
        new RecommendedContentDto(first, "첫째", "thumb1", 0.9),
        new RecommendedContentDto(second, "둘째", "thumb2", 0.8)), toolContext);

    assertThat(collector.getCards()).containsExactly(
        new ContentCardDto(first, "첫째", "thumb1"), new ContentCardDto(second, "둘째", "thumb2"));
  }
}
