package com.codeit.modoo_playlist.modulebatch.reviewsummary.processor;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryResult;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.batch.infrastructure.item.ItemProcessor;

@RequiredArgsConstructor
public class ReviewSummaryProcessor implements ItemProcessor<ReviewSummaryTarget, ReviewSummaryResult> {

  private final ChatClient chatClient;

  @Override
  public @Nullable ReviewSummaryResult process(ReviewSummaryTarget target) {
    String summary;
    try {
      summary = chatClient.prompt()
          .system(ReviewSummaryPromptBuilder.systemPrompt())
          .user(ReviewSummaryPromptBuilder.userPrompt(target))
          .call()
          .content();
    } catch (RuntimeException e) {
      throw new BaseException(ErrorCode.REVIEW_SUMMARY_GENERATION_FAILED, e);
    }

    if (summary == null || summary.isBlank()) {
      throw new BaseException(ErrorCode.REVIEW_SUMMARY_GENERATION_FAILED);
    }

    return new ReviewSummaryResult(target.contentId(), summary.trim());
  }
}
