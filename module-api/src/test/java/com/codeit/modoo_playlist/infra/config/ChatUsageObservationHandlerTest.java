package com.codeit.modoo_playlist.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.modoo_playlist.infra.config.ChatUsageObservationHandler.ChatUsageTotal;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.metadata.GoogleGenAiUsage;

class ChatUsageObservationHandlerTest {

  private final ObservationRegistry registry = new ChatUsageObservationConfig().observationRegistry();

  @Test
  void 툴_루프의_LLM_호출_사용량을_질문_단위로_합산한다() {
    ChatClientObservationContext clientContext = ChatClientObservationContext.builder()
        .request(ChatClientRequest.builder()
            .prompt(new Prompt("2010년대 일본 스릴러"))
            .context(Map.of(ChatMemory.CONVERSATION_ID, "conversation-1"))
            .build())
        .build();
    Observation client = Observation.start("spring.ai.chat.client", () -> clientContext, registry);

    modelCall(client, 1000, 30);
    modelCall(client, 1500, 400);
    client.stop();

    ChatUsageTotal total = clientContext.get(ChatUsageTotal.class);
    assertThat(total.calls()).isEqualTo(2);
    assertThat(total.input()).isEqualTo(2500);
    assertThat(total.output()).isEqualTo(430);
  }

  @Test
  void 스트리밍_기본_usage면_캐시와_thinking_토큰을_추정하지_않고_모르는_값으로_둔다() {
    // 스트리밍 합산 결과에는 input·output·total만 남는다. total의 나머지에는 tool-use 토큰도 섞일 수 있다
    DefaultUsage streaming = new DefaultUsage(1000, 30, 1100);

    assertThat(ChatUsageObservationHandler.cachedTokens(streaming)).isNull();
    assertThat(ChatUsageObservationHandler.thoughtsTokens(streaming)).isNull();

    ChatUsageTotal total = new ChatUsageTotal(System.nanoTime());
    total.add(geminiUsage(50, 200, 20));
    total.add(streaming);
    assertThat(total.cached()).isNull();
    assertThat(total.input()).isEqualTo(2000);
  }

  @Test
  void Gemini_확장_usage면_캐시와_thinking_토큰을_그대로_쓰고_tool_use_토큰을_thinking에_섞지_않는다() {
    GoogleGenAiUsage usage = geminiUsage(50, 200, 20);

    assertThat(ChatUsageObservationHandler.cachedTokens(usage)).isEqualTo(200);
    assertThat(ChatUsageObservationHandler.thoughtsTokens(usage)).isEqualTo(50);
    assertThat(ChatUsageObservationHandler.cachedTokens(geminiUsage(null, null, null))).isZero();

    ChatUsageTotal total = new ChatUsageTotal(System.nanoTime());
    total.add(usage);
    total.add(usage);
    assertThat(total.cached()).isEqualTo(400);
  }

  @Test
  void 챗_관측이_아닌_관측은_끄지_않고_핸들러만_건너뛴다() {
    assertThat(Observation.createNotStarted("spring.ai.chat.client", registry).isNoop()).isFalse();
    assertThat(Observation.createNotStarted("http.server.requests", registry).isNoop()).isFalse();
    assertThat(Observation.createNotStarted("spring.security.authorizations", registry).isNoop()).isFalse();

    ChatUsageObservationHandler handler = new ChatUsageObservationHandler();
    assertThat(handler.supportsContext(new Observation.Context())).isFalse();
    assertThat(handler.supportsContext(ChatModelObservationContext.builder()
        .prompt(new Prompt("질문")).provider("google-genai").build())).isTrue();
  }

  // input 1000, output 30. total에는 thinking과 tool-use 토큰이 함께 들어간다
  private static GoogleGenAiUsage geminiUsage(Integer thoughts, Integer cached, Integer toolUse) {
    int total = 1030 + (thoughts == null ? 0 : thoughts) + (toolUse == null ? 0 : toolUse);
    return new GoogleGenAiUsage(1000, 30, total, thoughts, cached, toolUse, null, null, null, null, null, null);
  }

  private void modelCall(Observation parent, int input, int output) {
    ChatModelObservationContext context = ChatModelObservationContext.builder()
        .prompt(new Prompt("질문"))
        .provider("google-genai")
        .build();
    Observation model = Observation.createNotStarted("gen_ai.client.operation", () -> context, registry)
        .parentObservation(parent)
        .start();
    context.setResponse(new ChatResponse(
        List.of(new Generation(new AssistantMessage("답변"))),
        ChatResponseMetadata.builder().usage(new DefaultUsage(input, output)).build()));
    model.stop();
  }
}
