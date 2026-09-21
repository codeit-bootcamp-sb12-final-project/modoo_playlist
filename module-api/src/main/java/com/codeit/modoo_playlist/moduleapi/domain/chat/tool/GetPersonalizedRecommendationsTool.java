package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.RecommendationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPersonalizedRecommendationsTool {
  private static final int DEFAULT_LIMIT = 5;

  private final RecommendationService recommendationService;
  private final ContentDetailResolver contentDetailResolver;

  @Tool(
      name = "get_personalized_recommendations",
      description = "로그인한 사용자와 취향이 비슷한 사람들이 좋아한 콘텐츠 기반으로 개인화 추천을 합니다. "
          + "결과에는 유형·연도·국가·태그·감독·출연진·줄거리 요약이 포함됩니다. "
          + "장르 등 기준이 명확하면 recommend_contents를, 취향 태그 자체가 궁금하면 get_user_preference를 쓰세요."
  )
  public List<ContentDetailDto> getPersonalizedRecommendations(ToolContext toolContext) {
    UUID userId = ChatToolContext.requireUserId(toolContext);
    log.info("get_personalized_recommendations 호출: userId={}", userId);

    List<RecommendedContentDto> result;
    try {
      result = recommendationService.getRecommendationsForMe(userId, DEFAULT_LIMIT);
    } catch (Exception e) {
      log.error("get_personalized_recommendations 조회 실패: userId={}", userId, e);
      return List.of();
    }
    log.info("get_personalized_recommendations 결과: {}건", result.size());

    return contentDetailResolver.resolve(result, toolContext);
  }
}
