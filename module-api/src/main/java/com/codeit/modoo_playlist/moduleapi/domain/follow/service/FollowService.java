package com.codeit.modoo_playlist.moduleapi.domain.follow.service;

import java.util.UUID;

public interface FollowService {

    void follow(UUID followerId, UUID followeeId);

    void unfollow(UUID followerId, UUID followeeId);

    boolean isFollowedByMe(UUID followerId, UUID followeeId);

    long countFollowers(UUID followeeId);

}