package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
import com.codeit.modoo_playlist.moduleapi.domain.chat.repository.ContentTitleRepository;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
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
class GetContentDetailToolTest {

  @Mock private ContentTitleRepository contentTitleRepository;
  @Mock private ContentDetailService contentDetailService;

  private final ToolContext emptyContext = new ToolContext(Map.of());

  private GetContentDetailTool tool() {
    return new GetContentDetailTool(contentDetailService, new ContentRefResolver(contentTitleRepository));
  }

  @Test
  void contentId가_없거나_형식이_잘못되면_빈_리스트를_반환한다() {
    assertThat(tool().getContentDetail(null, emptyContext)).isEmpty();
    assertThat(tool().getContentDetail("uuid-아님", emptyContext)).isEmpty();
    verifyNoInteractions(contentDetailService);
  }

  @Test
  void 상세정보가_있으면_그대로_반환한다() {
    UUID contentId = UUID.randomUUID();
    ContentDetailDto detail = ContentDetailDto.titleOnly(contentId, "제목");
    when(contentDetailService.getDetails(List.of(contentId))).thenReturn(List.of(detail));

    assertThat(tool().getContentDetail(contentId.toString(), emptyContext)).containsExactly(detail);
  }

  @Test
  void 제목으로_찾으면_상세정보를_반환하고_그_콘텐츠의_카드를_남긴다() {
    UUID contentId = UUID.randomUUID();
    ContentCardCollector collector = new ContentCardCollector();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, collector));
    ContentDetailDto detail = ContentDetailDto.titleOnly(contentId, "토이 스토리 5");
    when(contentTitleRepository.findCardsByNormalizedTitle("토이스토리5"))
        .thenReturn(List.of(new ContentCardDto(contentId, "토이 스토리 5", "thumb")));
    when(contentDetailService.getDetails(List.of(contentId))).thenReturn(List.of(detail));

    assertThat(tool().getContentDetail("토이 스토리 5", toolContext)).containsExactly(detail);
    assertThat(collector.getCards()).containsExactly(new ContentCardDto(contentId, "토이 스토리 5", "thumb"));
  }

  @Test
  void 콘텐츠가_없거나_삭제됐으면_빈_리스트를_반환한다() {
    UUID contentId = UUID.randomUUID();
    when(contentDetailService.getDetails(List.of(contentId))).thenReturn(List.of());

    assertThat(tool().getContentDetail(contentId.toString(), emptyContext)).isEmpty();
  }

  @Test
  void 조회_중_예외가_나면_삼키지_않고_전파한다() {
    UUID contentId = UUID.randomUUID();
    when(contentDetailService.getDetails(List.of(contentId))).thenThrow(new RuntimeException("DB 장애"));

    assertThatThrownBy(() -> tool().getContentDetail(contentId.toString(), emptyContext)).hasMessage("DB 장애");
  }
}
