package com.codeit.modoo_playlist.moduleapi.domain.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewSummaryService;
import com.codeit.modoo_playlist.moduleapi.dto.ReviewSummaryDto;

@ExtendWith(MockitoExtension.class)
class ReviewSummaryControllerTest {

  @Mock private ReviewSummaryService reviewSummaryService;
  @InjectMocks private ReviewSummaryController controller;

  @Test
  void 콘텐츠ID로_리뷰_요약을_조회한다() {
    UUID contentId = UUID.randomUUID();
    ReviewSummaryDto expected = new ReviewSummaryDto(contentId, "리뷰 요약입니다", Instant.now());
    when(reviewSummaryService.getReviewSummary(contentId)).thenReturn(expected);

    ResponseEntity<ReviewSummaryDto> response = controller.getReviewSummary(contentId);

    assertThat(response.getBody()).isEqualTo(expected);
  }
}
