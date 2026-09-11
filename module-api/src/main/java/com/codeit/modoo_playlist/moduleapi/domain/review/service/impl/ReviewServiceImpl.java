package com.codeit.modoo_playlist.moduleapi.domain.review.service.impl;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.review.entity.Review;
import com.codeit.modoo_playlist.core.domain.review.entity.ReviewStatus;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.mapper.ReviewMapper;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.ReviewRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.query.ReviewListCondition;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.query.ReviewQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.review.service.ReviewService;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.dto.review.request.ReviewListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.review.response.ReviewCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.review.response.ReviewResponse;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

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
    public ReviewResponse updateReview(UUID reviewId, UUID authorId, String text, BigDecimal rating) {
        Review review = getOwnedReview(reviewId, authorId);
        BigDecimal oldRating = review.getRating();

        review.update(text, rating);

        Content content = getExistingContent(review.getContentId());
        content.changeReview(oldRating, rating);

        return getReviewResponse(reviewId);
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

    @Override
    public ReviewResponse getReviewResponse(UUID reviewId) {
        Review review = getReview(reviewId);

        User author = userRepository.findById(review.getAuthorId())
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        UserSummaryResponse authorSummary = new UserSummaryResponse(
                author.getId(), author.getUsername(), author.getProfileImageUrl());

        return reviewMapper.toResponse(review, authorSummary);
    }

    @Override
    public ReviewCursorResponse getReviews(ReviewListRequest request) {
        ReviewListCondition condition = toCondition(request);

        ReviewQueryPage page = reviewRepository.findAllByCondition(condition);
        List<Review> reviews = page.reviews();

        if (reviews.isEmpty()) {
            return reviewMapper.toCursorResponse(page, List.of(), request.sortBy(), request.sortDirection());
        }

        Map<UUID, User> authorsById = userRepository
                .findAllById(reviews.stream().map(Review::getAuthorId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<ReviewResponse> data = reviews.stream()
                .map(review -> {
                    User author = authorsById.get(review.getAuthorId());
                    UserSummaryResponse authorSummary = (author == null)
                            ? null
                            : new UserSummaryResponse(author.getId(), author.getUsername(), author.getProfileImageUrl());
                    return reviewMapper.toResponse(review, authorSummary);
                })
                .toList();

        return reviewMapper.toCursorResponse(page, data, request.sortBy(), request.sortDirection());
    }

    private ReviewListCondition toCondition(ReviewListRequest request) {
        return new ReviewListCondition(
                request.contentId(),
                request.cursor(),
                request.idAfter(),
                request.limit(),
                ReviewListCondition.SortType.CREATED_AT,
                ReviewListCondition.SortDirection.valueOf(request.sortDirection())
        );
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