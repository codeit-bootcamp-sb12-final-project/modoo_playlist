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
  void 스트리밍_기본_usage면_thinking_토큰을_합계에서_입력과_출력을_빼서_구한다() {
    assertThat(ChatUsageObservationHandler.thoughtsTokens(new DefaultUsage(1000, 30, 1100))).isEqualTo(70);
    assertThat(ChatUsageObservationHandler.thoughtsTokens(new DefaultUsage(1000, 30))).isZero();
  }

  @Test
  void Spring_AI_관측만_통과시키고_다른_관측은_끈다() {
    assertThat(Observation.createNotStarted("spring.ai.chat.client", registry).isNoop()).isFalse();
    assertThat(Observation.createNotStarted("gen_ai.client.operation", registry).isNoop()).isFalse();
    assertThat(Observation.createNotStarted("spring.security.filterchains", registry).isNoop()).isTrue();
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
