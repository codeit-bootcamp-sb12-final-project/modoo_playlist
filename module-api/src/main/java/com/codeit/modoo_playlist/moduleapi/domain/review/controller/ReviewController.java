package com.codeit.modoo_playlist.moduleapi.domain.review.controller;

import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewService;
import com.codeit.modoo_playlist.moduleapi.dto.review.request.ReviewCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.review.request.ReviewListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.review.request.ReviewUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.review.response.ReviewCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.review.response.ReviewResponse;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ReviewCursorResponse> getReviews(
            @Valid @ModelAttribute ReviewListRequest request
    ) {
        return ResponseEntity.ok(reviewService.getReviews(request));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID reviewId = reviewService.createReview(
                request.contentId(), user.getUserDto().id(), request.text(), request.rating());
        ReviewResponse response = reviewService.getReviewResponse(reviewId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable UUID reviewId) {
        return ResponseEntity.ok(reviewService.getReviewResponse(reviewId));
    }

    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        ReviewResponse response = reviewService.updateReview(
                reviewId, user.getUserDto().id(), request.text(), request.rating());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserDetails user
    ) {
        reviewService.deleteReview(reviewId, user.getUserDto().id());
        return ResponseEntity.noContent().build();
    }

}