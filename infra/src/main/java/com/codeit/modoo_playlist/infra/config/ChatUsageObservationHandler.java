package com.codeit.modoo_playlist.infra.config;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.google.genai.metadata.GoogleGenAiUsage;

/**
 * 챗봇의 LLM 호출마다 토큰 사용량을 로그로 남기고, 질문 하나(툴 루프 전체)의 합계도 남긴다.
 */
@Slf4j
public class ChatUsageObservationHandler implements ObservationHandler<Observation.Context> {

  @Override
  public boolean supportsContext(Observation.Context context) {
    return context instanceof ChatModelObservationContext
        || context instanceof ChatClientObservationContext;
  }

  @Override
  public void onStart(Observation.Context context) {
    if (context instanceof ChatClientObservationContext) {
      context.put(ChatUsageTotal.class, new ChatUsageTotal(System.nanoTime()));
    }
  }

  @Override
  public void onStop(Observation.Context context) {
    if (context instanceof ChatModelObservationContext model) {
      onModelCallStop(model);
    } else if (context instanceof ChatClientObservationContext client) {
      onChatStop(client);
    }
  }

  private void onModelCallStop(ChatModelObservationContext context) {
    if (context.getResponse() == null) {
      return;
    }
    Usage usage = context.getResponse().getMetadata().getUsage();
    ChatUsageTotal total = findTotal(context);
    int call = total == null ? 0 : total.add(usage);
    // input  : 프롬프트 토큰. 시스템 프롬프트 + 툴 정의 + 대화 기억 + 이전 툴 결과가 모두 포함된다
    // output : 모델이 생성한 토큰 (툴 호출 요청 또는 답변 텍스트)
    // cached : input 중 Gemini 캐시에서 재사용된 토큰
    // thoughts : thinking 단계에서 쓴 토큰 (thinking-level: LOW)
    log.debug("chat-usage call={} input={} output={} cached={} thoughts={}",
        call, orZero(usage.getPromptTokens()), orZero(usage.getCompletionTokens()),
        cachedTokens(usage), thoughtsTokens(usage));
  }

  private void onChatStop(ChatClientObservationContext context) {
    ChatUsageTotal total = context.get(ChatUsageTotal.class);
    if (total == null) {
      return;
    }
    log.info("chat-usage total conversationId={} calls={} input={} output={} cached={} elapsedMs={}",
        context.getRequest().context().get(ChatMemory.CONVERSATION_ID),
        total.calls(), total.input(), total.output(), total.cached(), total.elapsedMs());
  }

  private static ChatUsageTotal findTotal(Observation.Context context) {
    ObservationView parent = context.getParentObservation();
    while (parent != null) {
      if (parent.getContextView() instanceof ChatClientObservationContext client) {
        return client.get(ChatUsageTotal.class);
      }
      parent = parent.getContextView().getParentObservation();
    }
    return null;
  }

  private static int cachedTokens(Usage usage) {
    return usage instanceof GoogleGenAiUsage gemini ? orZero(gemini.getCachedContentTokenCount()) : 0;
  }

  static int thoughtsTokens(Usage usage) {
    if (usage instanceof GoogleGenAiUsage gemini) {
      return orZero(gemini.getThoughtsTokenCount());
    }
    int rest = orZero(usage.getTotalTokens()) - orZero(usage.getPromptTokens()) - orZero(usage.getCompletionTokens());
    return Math.max(rest, 0);
  }

  private static int orZero(Integer value) {
    return value == null ? 0 : value;
  }

  static final class ChatUsageTotal {

    private final long startedAt;
    private int calls;
    private int input;
    private int output;
    private int cached;

    ChatUsageTotal(long startedAt) {
      this.startedAt = startedAt;
    }

    synchronized int add(Usage usage) {
      input += orZero(usage.getPromptTokens());
      output += orZero(usage.getCompletionTokens());
      cached += cachedTokens(usage);
      return ++calls;
    }

    synchronized int calls() {
      return calls;
    }

    synchronized int input() {
      return input;
    }

    synchronized int output() {
      return output;
    }

    synchronized int cached() {
      return cached;
    }

    long elapsedMs() {
      return (System.nanoTime() - startedAt) / 1_000_000;
    }
  }
}
