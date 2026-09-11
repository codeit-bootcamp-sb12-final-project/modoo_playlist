package com.codeit.modoo_playlist.moduleapi.domain.review.repository.query;

public interface ReviewQueryRepository {
    ReviewQueryPage findAllByCondition(ReviewListCondition condition);
}