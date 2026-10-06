package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.RecommendationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetTrendingTool {
  private static final int DEFAULT_LIMIT = 5;

  private final RecommendationService recommendationService;
  private final ContentDetailResolver contentDetailResolver;

  @Tool(
      name = "get_trending",
      description = "서비스에서 평점이 높은 인기 콘텐츠를 조회합니다('요즘 인기 작품', '평점 높은 콘텐츠'). "
          + "취향과 무관한 결과이므로 개인 맞춤 요청에는 get_personalized_recommendations를 먼저 쓰세요."
  )
  public List<ContentDetailDto> getTrending(ToolContext toolContext) {
    log.info("get_trending 호출");

    List<RecommendedContentDto> result = recommendationService.getTrendingContents(DEFAULT_LIMIT);
    log.info("get_trending 결과: {}건", result.size());

    return contentDetailResolver.resolve(result, toolContext);
  }
}
