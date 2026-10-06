package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.SemanticSearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchContentsTool {
  private static final int DEFAULT_LIMIT = 5;
  private static final int MAX_RETRIES = 1;

  private final SemanticSearchService semanticSearchService;
  private final ContentDetailResolver contentDetailResolver;

  @Tool(
      name = "search_contents",
      description = "분위기·줄거리·상황 같은 자연어 묘사나 감독·출연진·개봉 연도·제작 국가·스포츠 종목·리그·팀 이름으로 "
          + "콘텐츠를 의미 기반으로 검색합니다. 특정 콘텐츠와 비슷한 작품은 recommend_contents를 쓰세요."
  )
  public List<ContentDetailDto> searchContents(
      @ToolParam(description = "찾는 콘텐츠에 대한 자연어 설명. 이름·연도·국가 같은 조건은 사용자의 표현 그대로 포함") String query,
      @ToolParam(required = false, description = "직전 결과가 조건에 맞지 않아 검색어를 바꿔 다시 검색할 때만 true. 서로 다른 조건을 각각 검색할 때는 생략") Boolean retry,
      ToolContext toolContext
  ) {
    if (query == null || query.isBlank()) {
      log.warn("search_contents 호출: query가 비어 있어 검색을 건너뜁니다");
      return List.of();
    }

    boolean isRetry = Boolean.TRUE.equals(retry);
    ContentCardCollector collector = ChatToolContext.findCardCollector(toolContext);
    if (isRetry && collector != null) {
      if (collector.nextRetry() > MAX_RETRIES) {
        log.warn("search_contents 재검색 한도 초과");
        return List.of();
      }
      collector.clear();
    }
    log.info("search_contents 호출: retry={}, queryLength={}", isRetry, query.length());

    List<RecommendedContentDto> hits = semanticSearchService.search(query, DEFAULT_LIMIT);
    log.info("search_contents 결과: {}건", hits.size());

    return contentDetailResolver.resolve(hits, toolContext);
  }
}
