package com.codeit.modoo_playlist.modulebatch.reviewsummary.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget.ReviewItem;

class ReviewSummaryPromptBuilderTest {

  @Test
  void systemPrompt은_스포일러_금지_지시를_포함한다() {
    assertThat(ReviewSummaryPromptBuilder.systemPrompt()).contains("스포일러");
  }

  @Test
  void userPrompt은_제목과_리뷰건수와_평점_리뷰목록을_순서대로_담는다() {
    ReviewSummaryTarget target = new ReviewSummaryTarget("c1", "테스트 영화", List.of(
        new ReviewItem("좋아요", new BigDecimal("4.5")),
        new ReviewItem("별로예요", new BigDecimal("2.0"))
    ));

    String prompt = ReviewSummaryPromptBuilder.userPrompt(target);

    assertThat(prompt).isEqualTo(
        "[테스트 영화] 리뷰 2건:\n"
            + "- (평점 4.5) 좋아요\n"
            + "- (평점 2.0) 별로예요"
    );
  }

  @Test
  void 리뷰가_없으면_0건으로_표시하고_목록은_비운다() {
    ReviewSummaryTarget target = new ReviewSummaryTarget("c1", "리뷰없는 영화", List.of());

    String prompt = ReviewSummaryPromptBuilder.userPrompt(target);

    assertThat(prompt).isEqualTo("[리뷰없는 영화] 리뷰 0건:\n");
  }
}
