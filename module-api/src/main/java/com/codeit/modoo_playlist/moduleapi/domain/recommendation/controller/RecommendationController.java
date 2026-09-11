package com.codeit.modoo_playlist.moduleapi.domain.recommendation.controller;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendationQuery;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.SimilarContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.RecommendationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

  private final RecommendationService recommendationService;

  @GetMapping("/contents")
  public ResponseEntity<List<SimilarContentDto>> getSimilarContents(@Valid @ModelAttribute RecommendationQuery query) {
    return ResponseEntity.ok(recommendationService.getSimilarContents(query.contentId(), query.limit()));
  }
}
