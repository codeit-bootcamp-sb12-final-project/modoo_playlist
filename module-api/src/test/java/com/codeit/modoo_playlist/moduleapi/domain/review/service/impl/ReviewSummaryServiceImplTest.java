package com.codeit.modoo_playlist.moduleapi.domain.review.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.modoo_playlist.core.domain.review.entity.ContentReviewSummary;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.ReviewSummaryRepository;
import com.codeit.modoo_playlist.moduleapi.dto.ReviewSummaryDto;

@ExtendWith(MockitoExtension.class)
class ReviewSummaryServiceImplTest {

  @Mock private ReviewSummaryRepository reviewSummaryRepository;
  @Mock private ContentRepository contentRepository;
  @InjectMocks private ReviewSummaryServiceImpl service;

  @Test
  void 존재하지_않는_콘텐츠면_예외를_던진다() {
    UUID contentId = UUID.randomUUID();
    when(contentRepository.existsByIdAndDeletedAtIsNull(contentId)).thenReturn(false);

    assertThatThrownBy(() -> service.getReviewSummary(contentId))
        .isInstanceOf(BaseException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONTENT_NOT_FOUND);
  }

  @Test
  void 요약이_아직_없으면_summary와_updatedAt이_null인_dto를_반환한다() {
    UUID contentId = UUID.randomUUID();
    when(contentRepository.existsByIdAndDeletedAtIsNull(contentId)).thenReturn(true);
    when(reviewSummaryRepository.findById(contentId)).thenReturn(Optional.empty());

    ReviewSummaryDto result = service.getReviewSummary(contentId);

    assertThat(result.contentId()).isEqualTo(contentId);
    assertThat(result.summary()).isNull();
    assertThat(result.updatedAt()).isNull();
  }

  @Test
  void 요약이_있으면_저장된_요약과_갱신시각을_반환한다() {
    UUID contentId = UUID.randomUUID();
    Instant updatedAt = Instant.now();
    ContentReviewSummary entity = ContentReviewSummary.builder()
        .contentId(contentId)
        .summary("리뷰 요약입니다")
        .updatedAt(updatedAt)
        .build();
    when(contentRepository.existsByIdAndDeletedAtIsNull(contentId)).thenReturn(true);
    when(reviewSummaryRepository.findById(contentId)).thenReturn(Optional.of(entity));

    ReviewSummaryDto result = service.getReviewSummary(contentId);

    assertThat(result.contentId()).isEqualTo(contentId);
    assertThat(result.summary()).isEqualTo("리뷰 요약입니다");
    assertThat(result.updatedAt()).isEqualTo(updatedAt);
  }
}
