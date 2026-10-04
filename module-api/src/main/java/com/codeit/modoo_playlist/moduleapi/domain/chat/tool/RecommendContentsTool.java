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
  private final ContentRefResolver contentRefResolver;

  @Tool(
      name = "recommend_contents",
      description = "특정 콘텐츠와 비슷한 콘텐츠를 장르·태그 기반으로 추천합니다. 기준 콘텐츠의 ID(UUID) 또는 정확한 제목이 필요합니다. "
          + "자연어 묘사로 찾을 때는 search_contents를 쓰세요."
  )
  public List<ContentDetailDto> recommendContents(
      @ToolParam(description = "기준 콘텐츠의 ID(UUID) 또는 정확한 제목") String content,
      ToolContext toolContext
  ) {
    UUID id = contentRefResolver.resolve(content);
    if (id == null) {
      log.info("recommend_contents 호출: ID 형식이 아니거나 제목과 맞는 콘텐츠가 하나가 아닙니다. value={}", content);
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
