package com.codeit.modoo_playlist.moduleapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.infra.config.ChatClientConfig;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.SearchContentsTool;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.SemanticSearchService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;

class ChatClientConfigTest {

  private final ToolExecutionExceptionProcessor processor =
      new ChatClientConfig().toolExecutionExceptionProcessor();

  @Test
  void 툴_실행_실패는_내부_정보_없이_고정_안내문으로_변환한다() {
    ToolExecutionException exception = new ToolExecutionException(
        DefaultToolDefinition.builder().name("search_contents").description("d").inputSchema("{}").build(),
        new RuntimeException("jdbc:mysql://internal-host 접속 실패"));

    String result = processor.process(exception);

    assertThat(result).contains("조회할 수 없습니다").contains("다시 호출하지 말고");
    assertThat(result).doesNotContain("jdbc").doesNotContain("internal-host");
  }

  @Test
  void 툴이_예외를_던져도_ToolCallingManager가_고정_안내문을_모델_응답으로_돌려준다() {
    SemanticSearchService searchService = mock(SemanticSearchService.class);
    when(searchService.search("질의", 5)).thenThrow(new RuntimeException("jdbc:mysql://internal-host 접속 실패"));
    ToolCallingManager manager = DefaultToolCallingManager.builder()
        .toolExecutionExceptionProcessor(processor)
        .build();
    Prompt prompt = new Prompt(
        List.of(new UserMessage("질의")),
        ToolCallingChatOptions.builder()
            .toolCallbacks(ToolCallbacks.from(new SearchContentsTool(searchService, null)))
            .toolContext(Map.of("key", "value"))
            .build());
    AssistantMessage assistantMessage = AssistantMessage.builder()
        .toolCalls(List.of(new ToolCall("call-1", "function", "search_contents", "{\"query\":\"질의\"}")))
        .build();

    ToolExecutionResult result =
        manager.executeToolCalls(prompt, new ChatResponse(List.of(new Generation(assistantMessage))));

    List<Message> history = result.conversationHistory();
    ToolResponseMessage toolResponse = (ToolResponseMessage) history.get(history.size() - 1);
    String responseData = toolResponse.getResponses().get(0).responseData();
    assertThat(responseData).contains("조회할 수 없습니다").contains("다시 호출하지 말고");
    assertThat(responseData).doesNotContain("jdbc").doesNotContain("internal-host");
  }
}
