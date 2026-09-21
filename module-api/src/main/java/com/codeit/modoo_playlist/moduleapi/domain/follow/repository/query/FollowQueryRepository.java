package com.codeit.modoo_playlist.moduleapi.domain.follow.repository.query;

public interface FollowQueryRepository {

    FollowQueryPage findAllByCondition(FollowListCondition condition);
}