package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewSummaryService;
import com.codeit.modoo_playlist.moduleapi.dto.ReviewSummaryDto;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SummarizeReviewsToolTest {

  @Mock private ReviewSummaryService reviewSummaryService;

  private SummarizeReviewsTool tool() {
    return new SummarizeReviewsTool(reviewSummaryService);
  }

  @Test
  void contentId가_없거나_형식이_잘못되면_빈_리스트를_반환한다() {
    assertThat(tool().summarizeReviews(null)).isEmpty();
    assertThat(tool().summarizeReviews("uuid-아님")).isEmpty();
    verifyNoInteractions(reviewSummaryService);
  }

  @Test
  void 요약이_있으면_요약문_한_건을_반환한다() {
    UUID contentId = UUID.randomUUID();
    when(reviewSummaryService.getReviewSummary(contentId))
        .thenReturn(new ReviewSummaryDto(contentId, "연출이 좋다는 평이 많습니다.", Instant.now()));

    assertThat(tool().summarizeReviews(contentId.toString())).containsExactly("연출이 좋다는 평이 많습니다.");
  }

  @Test
  void 요약이_null이거나_비어있으면_빈_리스트를_반환한다() {
    UUID nullId = UUID.randomUUID();
    UUID blankId = UUID.randomUUID();
    when(reviewSummaryService.getReviewSummary(nullId)).thenReturn(new ReviewSummaryDto(nullId, null, null));
    when(reviewSummaryService.getReviewSummary(blankId)).thenReturn(new ReviewSummaryDto(blankId, " ", null));

    assertThat(tool().summarizeReviews(nullId.toString())).isEmpty();
    assertThat(tool().summarizeReviews(blankId.toString())).isEmpty();
  }

  @Test
  void 콘텐츠가_없으면_빈_리스트를_반환한다() {
    UUID contentId = UUID.randomUUID();
    when(reviewSummaryService.getReviewSummary(contentId))
        .thenThrow(new BaseException(ErrorCode.CONTENT_NOT_FOUND));

    assertThat(tool().summarizeReviews(contentId.toString())).isEmpty();
  }

  @Test
  void 콘텐츠_없음_외의_BaseException은_삼키지_않고_전파한다() {
    UUID contentId = UUID.randomUUID();
    when(reviewSummaryService.getReviewSummary(contentId))
        .thenThrow(new BaseException(ErrorCode.INTERNAL_SERVER_ERROR));

    assertThatThrownBy(() -> tool().summarizeReviews(contentId.toString()))
        .isInstanceOf(BaseException.class);
  }

  @Test
  void 그_외_예외도_삼키지_않고_전파한다() {
    UUID contentId = UUID.randomUUID();
    when(reviewSummaryService.getReviewSummary(contentId)).thenThrow(new RuntimeException("DB 장애"));

    assertThatThrownBy(() -> tool().summarizeReviews(contentId.toString())).hasMessage("DB 장애");
  }
}
