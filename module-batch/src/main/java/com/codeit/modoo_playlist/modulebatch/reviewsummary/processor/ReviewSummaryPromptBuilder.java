package com.codeit.modoo_playlist.modulebatch.reviewsummary.processor;

import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget;
import java.util.stream.Collectors;

public final class ReviewSummaryPromptBuilder {

  private static final String SYSTEM_PROMPT = """
      당신은 콘텐츠 리뷰를 요약하는 어시스턴트입니다.
      주어진 리뷰들을 읽고 공통적으로 언급되는 의견을 한국어 2~3문장으로 요약하세요.
      결말이나 반전 등 스포일러는 언급하지 마세요. 리뷰에 없는 내용은 지어내지 마세요.""";

  private ReviewSummaryPromptBuilder() {
  }

  public static String systemPrompt() {
    return SYSTEM_PROMPT;
  }

  public static String userPrompt(ReviewSummaryTarget target) {
    String reviewLines = target.reviews().stream()
        .map(review -> "- (평점 %s) %s".formatted(review.rating(), review.text()))
        .collect(Collectors.joining("\n"));
    return "[%s] 리뷰 %d건:\n%s".formatted(target.title(), target.reviews().size(), reviewLines);
  }
}
