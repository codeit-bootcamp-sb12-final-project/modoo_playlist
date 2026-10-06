package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
import com.codeit.modoo_playlist.moduleapi.domain.chat.repository.ContentTitleRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewSummaryService;
import com.codeit.modoo_playlist.moduleapi.dto.ReviewSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
class SummarizeReviewsToolTest {

  @Mock private ContentTitleRepository contentTitleRepository;
  @Mock private ReviewSummaryService reviewSummaryService;

  private final ToolContext emptyContext = new ToolContext(Map.of());

  private SummarizeReviewsTool tool() {
    return new SummarizeReviewsTool(reviewSummaryService, new ContentRefResolver(contentTitleRepository));
  }

  @Test
  void 콘텐츠를_특정하지_못하면_요약_없음과_구분되는_안내를_반환한다() {
    assertThat(tool().summarizeReviews(null, emptyContext))
        .containsExactly(SummarizeReviewsTool.CONTENT_NOT_FOUND_NOTICE);
    assertThat(tool().summarizeReviews("DB에 없는 제목", emptyContext))
        .containsExactly(SummarizeReviewsTool.CONTENT_NOT_FOUND_NOTICE);
    verifyNoInteractions(reviewSummaryService);
  }

  @Test
  void 제목으로_찾으면_요약을_반환하고_그_콘텐츠의_카드를_남긴다() {
    UUID contentId = UUID.randomUUID();
    ContentCardCollector collector = new ContentCardCollector();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, collector));
    when(contentTitleRepository.findCardsByNormalizedTitle("토이스토리5"))
        .thenReturn(List.of(new ContentCardDto(contentId, "토이 스토리 5", "thumb")));
    when(reviewSummaryService.getReviewSummary(contentId))
        .thenReturn(new ReviewSummaryDto(contentId, "연출이 좋다는 평이 많습니다.", Instant.now()));

    assertThat(tool().summarizeReviews("토이 스토리 5", toolContext)).containsExactly("연출이 좋다는 평이 많습니다.");
    assertThat(collector.getCards()).containsExactly(new ContentCardDto(contentId, "토이 스토리 5", "thumb"));
  }

  @Test
  void 요약이_있으면_요약문_한_건을_반환한다() {
    UUID contentId = UUID.randomUUID();
    when(reviewSummaryService.getReviewSummary(contentId))
        .thenReturn(new ReviewSummaryDto(contentId, "연출이 좋다는 평이 많습니다.", Instant.now()));

    assertThat(tool().summarizeReviews(contentId.toString(), emptyContext)).containsExactly("연출이 좋다는 평이 많습니다.");
  }

  @Test
  void 요약이_null이거나_비어있으면_빈_리스트를_반환한다() {
    UUID nullId = UUID.randomUUID();
    UUID blankId = UUID.randomUUID();
    when(reviewSummaryService.getReviewSummary(nullId)).thenReturn(new ReviewSummaryDto(nullId, null, null));
    when(reviewSummaryService.getReviewSummary(blankId)).thenReturn(new ReviewSummaryDto(blankId, " ", null));

    assertThat(tool().summarizeReviews(nullId.toString(), emptyContext)).isEmpty();
    assertThat(tool().summarizeReviews(blankId.toString(), emptyContext)).isEmpty();
  }

  @Test
  void 콘텐츠가_없으면_빈_리스트를_반환한다() {
    UUID contentId = UUID.randomUUID();
    when(reviewSummaryService.getReviewSummary(contentId))
        .thenThrow(new BaseException(ErrorCode.CONTENT_NOT_FOUND));

    assertThat(tool().summarizeReviews(contentId.toString(), emptyContext)).isEmpty();
  }

  @Test
  void 콘텐츠_없음_외의_BaseException은_삼키지_않고_전파한다() {
    UUID contentId = UUID.randomUUID();
    when(reviewSummaryService.getReviewSummary(contentId))
        .thenThrow(new BaseException(ErrorCode.INTERNAL_SERVER_ERROR));

    assertThatThrownBy(() -> tool().summarizeReviews(contentId.toString(), emptyContext))
        .isInstanceOf(BaseException.class);
  }

  @Test
  void 그_외_예외도_삼키지_않고_전파한다() {
    UUID contentId = UUID.randomUUID();
    when(reviewSummaryService.getReviewSummary(contentId)).thenThrow(new RuntimeException("DB 장애"));

    assertThatThrownBy(() -> tool().summarizeReviews(contentId.toString(), emptyContext)).hasMessage("DB 장애");
  }
}
