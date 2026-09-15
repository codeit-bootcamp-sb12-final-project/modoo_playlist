package com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search;

import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.ChatToolContext;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.ContentCardCollector;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.service.SearchContentsService;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.dto.SearchContentDto;
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

  private final SearchContentsService searchContentsService;

  @Tool(
      name = "search_contents",
      description = "사용자가 분위기, 줄거리, 상황 등을 자연어로 묘사하며 콘텐츠를 찾아달라고 할 때 "
          + "의미 기반으로 유사한 콘텐츠를 검색합니다. 장르/태그가 명확한 추천 요청에는 "
          + "대신 recommend_contents를 쓰세요."
  )
  public List<SearchContentDto> searchContents(
      @ToolParam(description = "사용자가 찾고 있는 콘텐츠에 대한 자연어 설명") String query,
      ToolContext toolContext
  ) {
    log.info("search_contents 호출: queryLength={}", query == null ? 0 : query.length());
    List<SearchContentDto> result = searchContentsService.search(query, DEFAULT_LIMIT);
    log.info("search_contents 결과: {}건", result.size());

    Object collector = toolContext.getContext().get(ChatToolContext.CARD_COLLECTOR);
    if (collector instanceof ContentCardCollector cardCollector) {
      result.forEach(c -> cardCollector.add(c.contentId(), c.title(), c.thumbnailUrl()));
    }
    return result;
  }
}
