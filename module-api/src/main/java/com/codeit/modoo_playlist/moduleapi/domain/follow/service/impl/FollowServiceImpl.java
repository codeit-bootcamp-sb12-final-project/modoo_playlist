package com.codeit.modoo_playlist.moduleapi.domain.follow.service.impl;

import com.codeit.modoo_playlist.core.domain.follow.entity.Follow;
import com.codeit.modoo_playlist.core.domain.user.dto.UserSummaryResponse;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.follow.repository.FollowRepository;
import com.codeit.modoo_playlist.moduleapi.domain.follow.repository.query.FollowListCondition;
import com.codeit.modoo_playlist.moduleapi.domain.follow.repository.query.FollowListType;
import com.codeit.modoo_playlist.moduleapi.domain.follow.repository.query.FollowQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.follow.service.FollowService;
import com.codeit.modoo_playlist.moduleapi.domain.notification.event.FollowedEvent;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.codeit.modoo_playlist.moduleapi.dto.follow.request.FollowListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.follow.response.FollowMemberCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.follow.response.FollowMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Follow follow(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BaseException(ErrorCode.SELF_FOLLOW_NOT_ALLOWED);
        }

        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new BaseException(ErrorCode.FOLLOW_ALREADY_EXISTS);
        }

        Follow follow = Follow.builder()
                .followerId(followerId)
                .followeeId(followeeId)
                .build();

        Follow saved = followRepository.save(follow);

        eventPublisher.publishEvent(new FollowedEvent(followerId, followeeId));

        return saved;
    }

    @Override
    @Transactional
    public void unfollow(UUID followId, UUID requesterId) {
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new BaseException(ErrorCode.FOLLOW_NOT_FOUND));

        if (!follow.getFollowerId().equals(requesterId)) {
            throw new BaseException(ErrorCode.FOLLOW_ACCESS_DENIED);
        }

        followRepository.delete(follow);
    }

    @Override
    public Follow getFollowStatus(UUID followerId, UUID followeeId) {
        return followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .orElseThrow(() -> new BaseException(ErrorCode.FOLLOW_NOT_FOUND));
    }

    @Override
    public long countFollowers(UUID followeeId) {
        return followRepository.countByFolloweeId(followeeId);
    }

    @Override
    public FollowMemberCursorResponse getFollowers(UUID followeeId, FollowListRequest request) {
        return getFollowMembers(followeeId, FollowListType.FOLLOWERS, request, Follow::getFollowerId);
    }

    @Override
    public FollowMemberCursorResponse getFollowing(UUID followerId, FollowListRequest request) {
        return getFollowMembers(followerId, FollowListType.FOLLOWING, request, Follow::getFolloweeId);
    }

    private FollowMemberCursorResponse getFollowMembers(
            UUID userId,
            FollowListType type,
            FollowListRequest request,
            Function<Follow, UUID> counterpartIdExtractor
    ) {
        FollowListCondition condition = new FollowListCondition(
                userId,
                type,
                request.cursor(),
                request.idAfter(),
                request.limit(),
                FollowListCondition.SortDirection.valueOf(request.sortDirection())
        );

        FollowQueryPage page = followRepository.findAllByCondition(condition);
        List<Follow> follows = page.follows();

        if (follows.isEmpty()) {
            return new FollowMemberCursorResponse(List.of(), page.nextCursor(), page.nextIdAfter(),
                    page.hasNext(), page.totalCount(), "createdAt", request.sortDirection());
        }

        Map<UUID, User> usersById = userRepository
                .findAllById(follows.stream().map(counterpartIdExtractor).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<FollowMemberResponse> data = follows.stream()
                .map(f -> {
                    User user = usersById.get(counterpartIdExtractor.apply(f));
                    UserSummaryResponse summary = (user == null)
                            ? null
                            : new UserSummaryResponse(user.getId(), user.getUsername(), user.getProfileImageUrl());
                    return new FollowMemberResponse(f.getId(), summary, f.getCreatedAt());
                })
                .toList();

        return new FollowMemberCursorResponse(data, page.nextCursor(), page.nextIdAfter(),
                page.hasNext(), page.totalCount(), "createdAt", request.sortDirection());
    }

}