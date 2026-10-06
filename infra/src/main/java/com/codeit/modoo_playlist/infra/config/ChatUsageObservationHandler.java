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
    // total  : Gemini가 준 합계. input + output 외에 thinking·tool-use 토큰이 들어 있다
    // cached : input 중 Gemini 캐시에서 재사용된 토큰
    // thoughts : thinking 단계에서 쓴 토큰 (thinking-level: LOW)
    // 스트리밍 응답은 Spring AI가 input·output·total만 남기고 합치므로 cached·thoughts는 알 수 없다. 0으로 적지 않고 n/a로 남긴다
    log.debug("chat-usage call={} input={} output={} total={} cached={} thoughts={}",
        call, orZero(usage.getPromptTokens()), orZero(usage.getCompletionTokens()), orZero(usage.getTotalTokens()),
        orNotAvailable(cachedTokens(usage)), orNotAvailable(thoughtsTokens(usage)));
  }

  private void onChatStop(ChatClientObservationContext context) {
    ChatUsageTotal total = context.get(ChatUsageTotal.class);
    if (total == null) {
      return;
    }
    log.info("chat-usage total conversationId={} calls={} input={} output={} cached={} elapsedMs={}",
        context.getRequest().context().get(ChatMemory.CONVERSATION_ID),
        total.calls(), total.input(), total.output(), orNotAvailable(total.cached()), total.elapsedMs());
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

  // Gemini 확장 사용량이 아니면(스트리밍) 알 수 없으므로 null
  static Integer cachedTokens(Usage usage) {
    return usage instanceof GoogleGenAiUsage gemini ? orZero(gemini.getCachedContentTokenCount()) : null;
  }

  static Integer thoughtsTokens(Usage usage) {
    return usage instanceof GoogleGenAiUsage gemini ? orZero(gemini.getThoughtsTokenCount()) : null;
  }

  private static int orZero(Integer value) {
    return value == null ? 0 : value;
  }

  private static Object orNotAvailable(Integer value) {
    return value == null ? "n/a" : value;
  }

  static final class ChatUsageTotal {

    private final long startedAt;
    private int calls;
    private int input;
    private int output;
    // 한 호출이라도 캐시 사용량을 모르면 합계도 모르는 값(null)이 된다
    private Integer cached = 0;

    ChatUsageTotal(long startedAt) {
      this.startedAt = startedAt;
    }

    synchronized int add(Usage usage) {
      input += orZero(usage.getPromptTokens());
      output += orZero(usage.getCompletionTokens());
      Integer callCached = cachedTokens(usage);
      cached = cached == null || callCached == null ? null : Integer.valueOf(cached + callCached);
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

    synchronized Integer cached() {
      return cached;
    }

    long elapsedMs() {
      return (System.nanoTime() - startedAt) / 1_000_000;
    }
  }
}
