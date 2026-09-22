package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.SemanticSearchService;
import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewSummaryService;
import com.codeit.modoo_playlist.moduleapi.dto.ReviewSummaryDto;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolExecutionException;

class ChatToolCallbackTest {

  @Test
  void 새_툴_3개가_고유한_이름의_콜백으로_등록된다() {
    ToolCallback[] callbacks = ToolCallbacks.from(
        new GetTrendingTool(null, null),
        new SummarizeReviewsTool(null),
        new GetContentDetailTool(null));

    assertThat(Arrays.stream(callbacks).map(c -> c.getToolDefinition().name()))
        .containsExactlyInAnyOrder("get_trending", "summarize_reviews", "get_content_detail");
  }

  @Test
  void summarize_reviews_결과가_JSON_배열로_직렬화된다() {
    UUID contentId = UUID.randomUUID();
    ReviewSummaryService reviewSummaryService = mock(ReviewSummaryService.class);
    when(reviewSummaryService.getReviewSummary(contentId))
        .thenReturn(new ReviewSummaryDto(contentId, "연출이 좋다는 평이 많습니다.", null));
    ToolCallback callback = ToolCallbacks.from(new SummarizeReviewsTool(reviewSummaryService))[0];

    String json = callback.call("{\"contentId\":\"" + contentId + "\"}");

    assertThat(json).isEqualTo("[\"연출이 좋다는 평이 많습니다.\"]");
  }

  @Test
  void 툴에서_난_예외는_ToolExecutionException으로_감싸져_전파된다() {
    SemanticSearchService semanticSearchService = mock(SemanticSearchService.class);
    when(semanticSearchService.search("질의", 5)).thenThrow(new RuntimeException("검색 엔진 장애"));
    ToolCallback callback =
        ToolCallbacks.from(new SearchContentsTool(semanticSearchService, null))[0];

    assertThatThrownBy(() -> callback.call("{\"query\":\"질의\"}", new ToolContext(Map.of("key", "value"))))
        .isInstanceOf(ToolExecutionException.class)
        .hasRootCauseMessage("검색 엔진 장애");
  }

  @Test
  void 요약이_없으면_빈_JSON_배열로_직렬화된다() {
    UUID contentId = UUID.randomUUID();
    ReviewSummaryService reviewSummaryService = mock(ReviewSummaryService.class);
    when(reviewSummaryService.getReviewSummary(contentId))
        .thenReturn(new ReviewSummaryDto(contentId, null, null));
    ToolCallback callback = ToolCallbacks.from(new SummarizeReviewsTool(reviewSummaryService))[0];

    String json = callback.call("{\"contentId\":\"" + contentId + "\"}");

    assertThat(json).isEqualTo("[]");
  }
}
