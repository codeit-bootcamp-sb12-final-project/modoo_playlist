package com.codeit.modoo_playlist.moduleapi.domain.recommendation.controller;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.HomeFeedResponse;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendationQuery;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.HomeFeedService;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.RecommendationService;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

  private final RecommendationService recommendationService;
  private final HomeFeedService homeFeedService;

  @GetMapping("/contents")
  public ResponseEntity<List<RecommendedContentDto>> getSimilarContents(
      @Valid @ModelAttribute RecommendationQuery query) {
    return ResponseEntity.ok(
        recommendationService.getSimilarContents(query.contentId(), query.limit()));
  }

  @PreAuthorize("hasRole('USER')")
  @GetMapping("/home")
  public ResponseEntity<HomeFeedResponse> getHomeFeed(@AuthenticationPrincipal UserDetails user) {
    return ResponseEntity.ok(homeFeedService.getHomeFeed(user.getUserDto().id()));
  }
}
