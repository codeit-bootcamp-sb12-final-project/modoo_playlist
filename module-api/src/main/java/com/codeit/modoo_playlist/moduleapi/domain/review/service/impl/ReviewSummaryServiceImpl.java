package com.codeit.modoo_playlist.moduleapi.domain.review.service.impl;

import com.codeit.modoo_playlist.core.domain.review.entity.ContentReviewSummary;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.ReviewSummaryRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewSummaryService;
import com.codeit.modoo_playlist.moduleapi.dto.ReviewSummaryDto;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewSummaryServiceImpl implements ReviewSummaryService {

  private final ReviewSummaryRepository reviewSummaryRepository;
  private final ContentRepository contentRepository;

  @Override
  public ReviewSummaryDto getReviewSummary(UUID contentId) {
    if (!contentRepository.existsById(contentId)) {
      throw new BaseException(ErrorCode.CONTENT_NOT_FOUND);
    }

    Optional<ContentReviewSummary> summary = reviewSummaryRepository.findById(contentId);
    if (summary.isEmpty()) {
      return new ReviewSummaryDto(contentId, null, null);
    }

    ContentReviewSummary entity = summary.get();
    return new ReviewSummaryDto(entity.getContentId(), entity.getSummary(), entity.getUpdatedAt());
  }
}
