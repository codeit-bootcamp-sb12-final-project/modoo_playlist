package com.codeit.modoo_playlist.moduleapi.domain.review.controller;

import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewSummaryService;
import com.codeit.modoo_playlist.moduleapi.dto.ReviewSummaryDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewSummaryController {

  private final ReviewSummaryService reviewSummaryService;

  @GetMapping("/summary/{contentId}")
  public ResponseEntity<ReviewSummaryDto> getReviewSummary(@PathVariable UUID contentId) {
    return ResponseEntity.ok(reviewSummaryService.getReviewSummary(contentId));
  }
}
