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
      description = "특정 콘텐츠에 대한 사용자 리뷰를 AI가 미리 요약해 둔 내용을 조회합니다. "
          + "사용자가 '평이 어때?', '사람들 반응은?'처럼 작품에 대한 평가를 물을 때 사용합니다. "
          + "콘텐츠 ID가 필요합니다(이전 대화나 다른 툴 결과에서 얻은 값). "
          + "요약이 없거나 콘텐츠를 찾을 수 없으면 빈 결과를 돌려줍니다."
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
