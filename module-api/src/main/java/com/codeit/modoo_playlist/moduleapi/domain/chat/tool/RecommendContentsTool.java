package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.RecommendationService;
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
public class RecommendContentsTool {
  private static final int DEFAULT_LIMIT = 5;

  private final RecommendationService recommendationService;
  private final ContentDetailResolver contentDetailResolver;

  @Tool(
      name = "recommend_contents",
      description = "특정 콘텐츠와 비슷한 콘텐츠를 장르/태그 기반으로 추천합니다. "
          + "기준 콘텐츠 ID가 필요합니다(이전 대화나 다른 툴 결과에서 얻은 값). "
          + "결과에는 유형·연도·국가·태그·감독·출연진·줄거리 요약이 포함됩니다. "
          + "분위기·줄거리 등 자연어 묘사로 콘텐츠를 찾는 요청에는 대신 search_contents를 쓰세요."
  )
  public List<ContentDetailDto> recommendContents(
      @ToolParam(description = "기준이 되는 콘텐츠 ID (UUID)") String contentId,
      ToolContext toolContext
  ) {
    UUID id = ChatToolContext.parseUuid(contentId);
    if (id == null) {
      log.warn("recommend_contents 호출: contentId가 없거나 형식이 올바르지 않습니다. value={}", contentId);
      return List.of();
    }
    log.info("recommend_contents 호출: contentId={}", id);

    List<RecommendedContentDto> result;
    try {
      result = recommendationService.getSimilarContents(id, DEFAULT_LIMIT);
    } catch (BaseException e) {
      if (e.getErrorCode() == ErrorCode.CONTENT_NOT_FOUND) {
        return List.of();
      }
      throw e;
    }
    log.info("recommend_contents 결과: {}건", result.size());

    return contentDetailResolver.resolve(result, toolContext);
  }
}
