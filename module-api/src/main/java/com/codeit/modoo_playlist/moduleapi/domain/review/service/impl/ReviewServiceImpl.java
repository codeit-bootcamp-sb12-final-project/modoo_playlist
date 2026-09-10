package com.codeit.modoo_playlist.moduleapi.domain.review.service.impl;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.review.entity.Review;
import com.codeit.modoo_playlist.core.domain.review.entity.ReviewStatus;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.ReviewRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewService;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ContentRepository contentRepository;

    @Override
    @Transactional
    public UUID createReview(UUID contentId, UUID authorId, String text, BigDecimal rating) {
        Content content = getExistingContent(contentId);

        if (reviewRepository.existsByContentIdAndAuthorId(contentId, authorId)) {
            throw new BaseException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
                .contentId(contentId)
                .authorId(authorId)
                .text(text)
                .rating(rating)
                .status(ReviewStatus.COMPLETED)
                .build();

        Review saved = reviewRepository.save(review);

        content.addReview(rating);

        return saved.getId();
    }

    @Override
    @Transactional
    public void updateReview(UUID reviewId, UUID authorId, String text, BigDecimal rating) {
        Review review = getOwnedReview(reviewId, authorId);
        BigDecimal oldRating = review.getRating();

        review.update(text, rating);

        Content content = getExistingContent(review.getContentId());
        content.changeReview(oldRating, rating);
    }

    @Override
    @Transactional
    public void deleteReview(UUID reviewId, UUID authorId) {
        Review review = getOwnedReview(reviewId, authorId);

        reviewRepository.delete(review);

        Content content = getExistingContent(review.getContentId());
        content.removeReview(review.getRating());
    }

    @Override
    public Review getReview(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BaseException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private Review getOwnedReview(UUID reviewId, UUID authorId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BaseException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getAuthorId().equals(authorId)) {
            throw new BaseException(ErrorCode.REVIEW_ACCESS_DENIED);
        }

        return review;
    }

    private Content getExistingContent(UUID contentId) {
        return contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new NoSuchElementException("콘텐츠를 찾을 수 없습니다."));
    }

}