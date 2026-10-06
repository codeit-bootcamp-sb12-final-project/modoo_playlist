package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewSummaryService;
import com.codeit.modoo_playlist.moduleapi.dto.ReviewSummaryDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummarizeReviewsTool {

  // 빈 결과는 "요약 없음"이라는 뜻이라, 콘텐츠를 못 찾은 경우는 모델이 구분할 수 있게 따로 알린다
  static final String CONTENT_NOT_FOUND_NOTICE = "콘텐츠를 찾지 못했습니다(리뷰 요약이 없다는 뜻이 아님). search_contents로 찾은 뒤 그 ID로 다시 호출하세요.";

  private final ReviewSummaryService reviewSummaryService;
  private final ContentRefResolver contentRefResolver;

  @Tool(
      name = "summarize_reviews",
      description = "특정 콘텐츠의 AI 리뷰 요약을 조회합니다. '평이 어때?', '사람들 반응은?' 같은 평가 질문에 쓰며 콘텐츠의 ID(UUID) 또는 정확한 제목이 필요합니다. "
          + "요약이 없으면 빈 결과입니다."
  )
  public List<String> summarizeReviews(
      @ToolParam(description = "리뷰 요약을 조회할 콘텐츠의 ID(UUID) 또는 정확한 제목") String content,
      ToolContext toolContext
  ) {
    UUID id = contentRefResolver.resolve(content, toolContext);
    if (id == null) {
      log.info("summarize_reviews 호출: ID 형식이 아니거나 제목과 맞는 콘텐츠가 하나가 아닙니다. value={}", content);
      return List.of(CONTENT_NOT_FOUND_NOTICE);
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
