package com.codeit.modoo_playlist.modulebatch.reviewsummary.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryResult;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget.ReviewItem;

@ExtendWith(MockitoExtension.class)
class ReviewSummaryProcessorTest {

  @Mock private ChatClient chatClient;
  @Mock private ChatClient.ChatClientRequestSpec requestSpec;
  @Mock private ChatClient.CallResponseSpec callResponseSpec;

  @Test
  void 요약을_받으면_공백을_다듬어_결과로_반환한다() throws Exception {
    stubChatChainReturning("  요약 결과입니다  ");

    ReviewSummaryResult result = new ReviewSummaryProcessor(chatClient).process(target());

    assertThat(result.contentId()).isEqualTo("c1");
    assertThat(result.summary()).isEqualTo("요약 결과입니다");
  }

  @Test
  void 응답이_빈_문자열이면_예외를_던진다() {
    stubChatChainReturning("   ");

    assertThatThrownBy(() -> new ReviewSummaryProcessor(chatClient).process(target()))
        .isInstanceOf(BaseException.class)
        .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
            .isEqualTo(ErrorCode.REVIEW_SUMMARY_GENERATION_FAILED));
  }

  @Test
  void 응답이_null이면_예외를_던진다() {
    stubChatChainReturning(null);

    assertThatThrownBy(() -> new ReviewSummaryProcessor(chatClient).process(target()))
        .isInstanceOf(BaseException.class)
        .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
            .isEqualTo(ErrorCode.REVIEW_SUMMARY_GENERATION_FAILED));
  }

  @Test
  void LLM_호출이_실패하면_원인을_담아_예외로_감싼다() {
    RuntimeException cause = new RuntimeException("Gemini 호출 실패");
    when(chatClient.prompt()).thenThrow(cause);

    assertThatThrownBy(() -> new ReviewSummaryProcessor(chatClient).process(target()))
        .isInstanceOf(BaseException.class)
        .hasCause(cause)
        .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
            .isEqualTo(ErrorCode.REVIEW_SUMMARY_GENERATION_FAILED));
  }

  private void stubChatChainReturning(String content) {
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.system(anyString())).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn(content);
  }

  private ReviewSummaryTarget target() {
    return new ReviewSummaryTarget("c1", "제목", List.of(new ReviewItem("좋아요", new BigDecimal("4.0"))));
  }
}
