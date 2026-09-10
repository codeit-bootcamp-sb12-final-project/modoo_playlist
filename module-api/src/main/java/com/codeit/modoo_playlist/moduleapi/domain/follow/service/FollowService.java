package com.codeit.modoo_playlist.moduleapi.domain.follow.service;

import com.codeit.modoo_playlist.core.domain.follow.entity.Follow;

import java.util.UUID;

public interface FollowService {

    Follow follow(UUID followerId, UUID followeeId);

    void unfollow(UUID followId, UUID requesterId);

    Follow getFollowStatus(UUID followerId, UUID followeeId);

    long countFollowers(UUID followeeId);

}