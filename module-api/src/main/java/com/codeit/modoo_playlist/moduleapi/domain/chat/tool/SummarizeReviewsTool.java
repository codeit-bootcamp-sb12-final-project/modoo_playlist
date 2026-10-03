package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewSummaryService;
import com.codeit.modoo_playlist.moduleapi.dto.ReviewSummaryDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummarizeReviewsTool {

  private final ReviewSummaryService reviewSummaryService;

  @Tool(
      name = "summarize_reviews",
      description = "특정 콘텐츠의 AI 리뷰 요약을 조회합니다. '평이 어때?', '사람들 반응은?' 같은 평가 질문에 쓰며 콘텐츠 ID가 필요합니다. "
          + "요약이 없으면 빈 결과입니다."
  )
  public List<String> summarizeReviews(
      @ToolParam(description = "리뷰 요약을 조회할 콘텐츠 ID (UUID)") String contentId
  ) {
    UUID id = ChatToolContext.parseUuid(contentId);
    if (id == null) {
      log.warn("summarize_reviews 호출: contentId가 없거나 형식이 올바르지 않습니다. value={}", contentId);
      return List.of();
    }
    log.info("summarize_reviews 호출: contentId={}", id);

    try {
      ReviewSummaryDto dto = reviewSummaryService.getReviewSummary(id);
      List<String> result = dto.summary() == null || dto.summary().isBlank()
          ? List.of()
          : List.of(dto.summary());
      log.info("summarize_reviews 결과: {}건", result.size());
      return result;
    } catch (BaseException e) {
      if (e.getErrorCode() == ErrorCode.CONTENT_NOT_FOUND) {
        return List.of();
      }
      throw e;
    }
  }
}
