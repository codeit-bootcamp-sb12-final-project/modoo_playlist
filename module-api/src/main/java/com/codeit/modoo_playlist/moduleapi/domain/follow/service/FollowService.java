package com.codeit.modoo_playlist.moduleapi.domain.follow.service;

import com.codeit.modoo_playlist.core.domain.follow.entity.Follow;
import com.codeit.modoo_playlist.moduleapi.dto.follow.request.FollowListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.follow.response.FollowMemberCursorResponse;

import java.util.UUID;

public interface FollowService {

    Follow follow(UUID followerId, UUID followeeId);

    void unfollow(UUID followId, UUID requesterId);

    Follow getFollowStatus(UUID followerId, UUID followeeId);

    long countFollowers(UUID followeeId);

    FollowMemberCursorResponse getFollowers(UUID followeeId, FollowListRequest request);

    FollowMemberCursorResponse getFollowing(UUID followerId, FollowListRequest request);

}