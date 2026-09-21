package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.ContentDetailService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentDetailResolver {

  private final ContentDetailService contentDetailService;

  public List<ContentDetailDto> resolve(List<RecommendedContentDto> hits, ToolContext toolContext) {
    List<ContentDetailDto> details = fetchDetails(hits);
    collectCards(hits, details, toolContext);
    return details;
  }

  private List<ContentDetailDto> fetchDetails(List<RecommendedContentDto> hits) {
    try {
      return contentDetailService.getDetails(hits.stream().map(RecommendedContentDto::contentId).toList());
    } catch (Exception e) {
      log.error("콘텐츠 상세 조회 실패, 제목만 반환합니다", e);
      return hits.stream()
          .map(hit -> ContentDetailDto.titleOnly(hit.contentId(), hit.title()))
          .toList();
    }
  }

  private void collectCards(List<RecommendedContentDto> hits, List<ContentDetailDto> details, ToolContext toolContext) {
    Object collector = toolContext.getContext().get(ChatToolContext.CARD_COLLECTOR);
    if (!(collector instanceof ContentCardCollector cardCollector)) {
      return;
    }
    Set<UUID> resolvedIds = details.stream().map(ContentDetailDto::contentId).collect(Collectors.toSet());
    hits.stream()
        .filter(hit -> resolvedIds.contains(hit.contentId()))
        .forEach(hit -> cardCollector.add(hit.contentId(), hit.title(), hit.thumbnailUrl()));
  }
}
