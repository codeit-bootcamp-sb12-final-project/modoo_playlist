package com.codeit.modoo_playlist.moduleapi.domain.review.service;

import com.codeit.modoo_playlist.moduleapi.dto.ReviewSummaryDto;
import java.util.UUID;

public interface ReviewSummaryService {

  ReviewSummaryDto getReviewSummary(UUID contentId);
}
