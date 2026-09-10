package com.codeit.modoo_playlist.moduleapi.domain.review.controller;

import com.codeit.modoo_playlist.core.domain.review.entity.Review;
import com.codeit.modoo_playlist.moduleapi.domain.review.mapper.ReviewMapper;
import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewService;
import com.codeit.modoo_playlist.moduleapi.dto.review.request.ReviewCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.review.request.ReviewUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.review.response.ReviewResponse;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID reviewId = reviewService.createReview(request.contentId(), user.getUserDto().id(), request.text(), request.rating());
        Review review = reviewService.getReview(reviewId);
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewMapper.toResponse(review));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable UUID reviewId) {
        Review review = reviewService.getReview(reviewId);
        return ResponseEntity.ok(reviewMapper.toResponse(review));
    }

    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{reviewId}")
    public ResponseEntity<Void> updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        reviewService.updateReview(reviewId, user.getUserDto().id(), request.text(), request.rating());
        return ResponseEntity.noContent().build();
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