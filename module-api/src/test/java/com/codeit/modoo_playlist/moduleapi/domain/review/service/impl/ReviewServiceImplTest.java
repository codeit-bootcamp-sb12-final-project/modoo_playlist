package com.codeit.modoo_playlist.moduleapi.domain.review.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.dto.review.request.ReviewListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.review.response.ReviewCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.review.response.ReviewResponse;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReviewMapper reviewMapper;
    @InjectMocks private ReviewServiceImpl reviewService;

    @Test
    void 리뷰를_생성하면_저장되고_콘텐츠_평점이_갱신된다() {
        UUID contentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        BigDecimal rating = BigDecimal.valueOf(4.5);
        Content content = content();

        when(contentRepository.findByIdAndDeletedAtIsNull(contentId)).thenReturn(Optional.of(content));
        when(reviewRepository.existsByContentIdAndAuthorId(contentId, authorId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class)))
                .thenReturn(review(reviewId, contentId, authorId, "재밌어요", rating));

        UUID result = reviewService.createReview(contentId, authorId, "재밌어요", rating);

        assertThat(result).isEqualTo(reviewId);
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getContentId()).isEqualTo(contentId);
        assertThat(captor.getValue().getAuthorId()).isEqualTo(authorId);
        assertThat(captor.getValue().getText()).isEqualTo("재밌어요");
        assertThat(captor.getValue().getRating()).isEqualByComparingTo(rating);
        assertThat(captor.getValue().getStatus()).isEqualTo(ReviewStatus.COMPLETED);
        assertThat(content.getReviewCount()).isEqualTo(1);
        assertThat(content.getAverageRating()).isEqualByComparingTo(rating);
    }

    @Test
    void 이미_리뷰를_작성한_콘텐츠에_다시_작성하면_REVIEW_ALREADY_EXISTS를_반환한다() {
        UUID contentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Content content = content();
        when(contentRepository.findByIdAndDeletedAtIsNull(contentId)).thenReturn(Optional.of(content));
        when(reviewRepository.existsByContentIdAndAuthorId(contentId, authorId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(contentId, authorId, "재밌어요", BigDecimal.valueOf(4.5)))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS));
        verify(reviewRepository, never()).save(any(Review.class));
        assertThat(content.getReviewCount()).isZero();
    }

    @Test
    void 존재하지_않는_콘텐츠에_리뷰를_생성하면_NoSuchElementException이_발생한다() {
        UUID contentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        when(contentRepository.findByIdAndDeletedAtIsNull(contentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(contentId, authorId, "재밌어요", BigDecimal.valueOf(4.5)))
                .isInstanceOf(NoSuchElementException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void 리뷰를_수정하면_내용과_콘텐츠_평점이_갱신되고_응답을_반환한다() {
        UUID reviewId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Review review = review(reviewId, contentId, authorId, "이전 리뷰", BigDecimal.valueOf(3.0));
        Content content = content(1, BigDecimal.valueOf(3.0));
        User author = user(authorId, "jhdb");
        UserSummaryResponse authorSummary =
                new UserSummaryResponse(author.getId(), author.getUsername(), author.getProfileImageUrl());
        ReviewResponse expected =
                new ReviewResponse(reviewId, contentId, authorSummary, "수정된 리뷰", BigDecimal.valueOf(4.0));

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(contentRepository.findByIdAndDeletedAtIsNull(contentId)).thenReturn(Optional.of(content));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(reviewMapper.toResponse(review, authorSummary)).thenReturn(expected);

        ReviewResponse result = reviewService.updateReview(reviewId, authorId, "수정된 리뷰", BigDecimal.valueOf(4.0));

        assertThat(result).isEqualTo(expected);
        assertThat(review.getText()).isEqualTo("수정된 리뷰");
        assertThat(review.getRating()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
        assertThat(content.getReviewCount()).isEqualTo(1);
        assertThat(content.getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
    }

    @Test
    void 존재하지_않는_리뷰를_수정하면_REVIEW_NOT_FOUND를_반환한다() {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, UUID.randomUUID(), "수정", BigDecimal.valueOf(4.0)))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    @Test
    void 본인_소유가_아닌_리뷰를_수정하면_REVIEW_ACCESS_DENIED를_반환한다() {
        UUID reviewId = UUID.randomUUID();
        Review review = review(reviewId, UUID.randomUUID(), UUID.randomUUID(), "리뷰", BigDecimal.valueOf(3.0));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, UUID.randomUUID(), "수정", BigDecimal.valueOf(4.0)))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ACCESS_DENIED));
        verify(contentRepository, never()).findByIdAndDeletedAtIsNull(any(UUID.class));
    }

    @Test
    void 리뷰를_삭제하면_저장소에서_삭제되고_콘텐츠_평점이_갱신된다() {
        UUID reviewId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Review review = review(reviewId, contentId, authorId, "리뷰", BigDecimal.valueOf(4.5));
        Content content = content(2, BigDecimal.valueOf(7.5));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(contentRepository.findByIdAndDeletedAtIsNull(contentId)).thenReturn(Optional.of(content));

        reviewService.deleteReview(reviewId, authorId);

        verify(reviewRepository).delete(review);
        assertThat(content.getReviewCount()).isEqualTo(1);
        assertThat(content.getAverageRating()).isEqualByComparingTo(BigDecimal.valueOf(3.0));
    }

    @Test
    void 존재하지_않는_리뷰를_삭제하면_REVIEW_NOT_FOUND를_반환한다() {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, UUID.randomUUID()))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void 본인_소유가_아닌_리뷰를_삭제하면_REVIEW_ACCESS_DENIED를_반환한다() {
        UUID reviewId = UUID.randomUUID();
        Review review = review(reviewId, UUID.randomUUID(), UUID.randomUUID(), "리뷰", BigDecimal.valueOf(4.5));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, UUID.randomUUID()))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ACCESS_DENIED));
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void 리뷰_단건_조회는_존재하면_반환한다() {
        UUID reviewId = UUID.randomUUID();
        Review review = review(reviewId, UUID.randomUUID(), UUID.randomUUID(), "리뷰", BigDecimal.valueOf(4.0));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThat(reviewService.getReview(reviewId)).isSameAs(review);
    }

    @Test
    void 리뷰_단건_조회는_존재하지_않으면_REVIEW_NOT_FOUND를_반환한다() {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReview(reviewId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    @Test
    void 리뷰_응답_조회는_작성자_정보를_포함해_반환한다() {
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Review review = review(reviewId, UUID.randomUUID(), authorId, "리뷰", BigDecimal.valueOf(4.0));
        User author = user(authorId, "jhdb");
        UserSummaryResponse authorSummary =
                new UserSummaryResponse(author.getId(), author.getUsername(), author.getProfileImageUrl());
        ReviewResponse expected =
                new ReviewResponse(reviewId, review.getContentId(), authorSummary, "리뷰", BigDecimal.valueOf(4.0));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(reviewMapper.toResponse(review, authorSummary)).thenReturn(expected);

        assertThat(reviewService.getReviewResponse(reviewId)).isEqualTo(expected);
    }

    @Test
    void 리뷰_응답_조회는_작성자가_없으면_USER_NOT_FOUND를_반환한다() {
        UUID reviewId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Review review = review(reviewId, UUID.randomUUID(), authorId, "리뷰", BigDecimal.valueOf(4.0));
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(userRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewResponse(reviewId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void 리뷰_목록_조회_결과가_없으면_빈_커서_응답을_반환한다() {
        UUID contentId = UUID.randomUUID();
        ReviewListRequest request = new ReviewListRequest(contentId, null, null, 20, "DESCENDING", "createdAt");
        ReviewQueryPage page = new ReviewQueryPage(List.of(), null, null, false, 0L);
        ReviewCursorResponse expected =
                new ReviewCursorResponse(List.of(), null, null, false, 0L, "createdAt", "DESCENDING");
        when(reviewRepository.findAllByCondition(any(ReviewListCondition.class))).thenReturn(page);
        when(reviewMapper.toCursorResponse(page, List.of(), "createdAt", "DESCENDING")).thenReturn(expected);

        ReviewCursorResponse result = reviewService.getReviews(request);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<ReviewListCondition> captor = ArgumentCaptor.forClass(ReviewListCondition.class);
        verify(reviewRepository).findAllByCondition(captor.capture());
        assertThat(captor.getValue().contentId()).isEqualTo(contentId);
        assertThat(captor.getValue().sortBy()).isEqualTo(ReviewListCondition.SortType.CREATED_AT);
        assertThat(captor.getValue().sortDirection()).isEqualTo(ReviewListCondition.SortDirection.DESCENDING);
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void 리뷰_목록_조회는_작성자_정보를_배치로_조회해_응답에_포함한다() {
        UUID contentId = UUID.randomUUID();
        UUID authorId1 = UUID.randomUUID();
        UUID authorId2 = UUID.randomUUID();
        Review review1 = review(UUID.randomUUID(), contentId, authorId1, "리뷰1", BigDecimal.valueOf(4.0));
        Review review2 = review(UUID.randomUUID(), contentId, authorId2, "리뷰2", BigDecimal.valueOf(3.5));
        User author1 = user(authorId1, "user1");
        User author2 = user(authorId2, "user2");
        UserSummaryResponse authorSummary1 =
                new UserSummaryResponse(author1.getId(), author1.getUsername(), author1.getProfileImageUrl());
        UserSummaryResponse authorSummary2 =
                new UserSummaryResponse(author2.getId(), author2.getUsername(), author2.getProfileImageUrl());
        ReviewResponse response1 = new ReviewResponse(review1.getId(), contentId, authorSummary1, "리뷰1", BigDecimal.valueOf(4.0));
        ReviewResponse response2 = new ReviewResponse(review2.getId(), contentId, authorSummary2, "리뷰2", BigDecimal.valueOf(3.5));
        ReviewListRequest request = new ReviewListRequest(contentId, null, null, 20, "DESCENDING", "createdAt");
        ReviewQueryPage page = new ReviewQueryPage(List.of(review1, review2), "cursor", review2.getId(), true, 2L);
        ReviewCursorResponse expected = new ReviewCursorResponse(
                List.of(response1, response2), "cursor", review2.getId(), true, 2L, "createdAt", "DESCENDING");
        when(reviewRepository.findAllByCondition(any(ReviewListCondition.class))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of(author1, author2));
        when(reviewMapper.toResponse(review1, authorSummary1)).thenReturn(response1);
        when(reviewMapper.toResponse(review2, authorSummary2)).thenReturn(response2);
        when(reviewMapper.toCursorResponse(page, List.of(response1, response2), "createdAt", "DESCENDING"))
                .thenReturn(expected);

        ReviewCursorResponse result = reviewService.getReviews(request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void 리뷰_목록_조회는_작성자_정보가_없으면_author를_null로_매핑한다() {
        UUID contentId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Review review = review(UUID.randomUUID(), contentId, authorId, "리뷰", BigDecimal.valueOf(4.0));
        ReviewResponse response = new ReviewResponse(review.getId(), contentId, null, "리뷰", BigDecimal.valueOf(4.0));
        ReviewListRequest request = new ReviewListRequest(contentId, null, null, 20, "DESCENDING", "createdAt");
        ReviewQueryPage page = new ReviewQueryPage(List.of(review), null, null, false, 1L);
        ReviewCursorResponse expected =
                new ReviewCursorResponse(List.of(response), null, null, false, 1L, "createdAt", "DESCENDING");
        when(reviewRepository.findAllByCondition(any(ReviewListCondition.class))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of());
        when(reviewMapper.toResponse(review, null)).thenReturn(response);
        when(reviewMapper.toCursorResponse(page, List.of(response), "createdAt", "DESCENDING")).thenReturn(expected);

        ReviewCursorResponse result = reviewService.getReviews(request);

        assertThat(result).isEqualTo(expected);
    }

    private Review review(UUID id, UUID contentId, UUID authorId, String text, BigDecimal rating) {
        return Review.builder()
                .id(id)
                .contentId(contentId)
                .authorId(authorId)
                .text(text)
                .rating(rating)
                .status(ReviewStatus.COMPLETED)
                .build();
    }

    private Content content() {
        return content(0, BigDecimal.ZERO);
    }

    private Content content(int reviewCount, BigDecimal ratingSum) {
        BigDecimal averageRating = reviewCount == 0
                ? BigDecimal.ZERO
                : ratingSum.divide(BigDecimal.valueOf(reviewCount), 1, RoundingMode.HALF_UP);
        return Content.builder()
                .reviewCount(reviewCount)
                .ratingSum(ratingSum)
                .averageRating(averageRating)
                .build();
    }

    private User user(UUID id, String username) {
        User user = User.create(username + "@example.com", username, "encoded-password");
        user.updateProfile(username, "https://example.com/" + username + ".png");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}