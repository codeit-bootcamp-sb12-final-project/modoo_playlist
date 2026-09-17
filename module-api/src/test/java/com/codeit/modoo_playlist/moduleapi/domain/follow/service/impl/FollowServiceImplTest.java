package com.codeit.modoo_playlist.moduleapi.domain.follow.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.modoo_playlist.core.domain.follow.entity.Follow;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.follow.repository.FollowRepository;

@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock private FollowRepository followRepository;
    @InjectMocks private FollowServiceImpl followService;

    @Test
    void 팔로우하면_관계가_저장된다() {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
                .thenReturn(false);
        when(followRepository.save(any(Follow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Follow result = followService.follow(followerId, followeeId);

        assertThat(result.getFollowerId()).isEqualTo(followerId);
        assertThat(result.getFolloweeId()).isEqualTo(followeeId);
        ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
        verify(followRepository).save(captor.capture());
        assertThat(captor.getValue().getFollowerId()).isEqualTo(followerId);
        assertThat(captor.getValue().getFolloweeId()).isEqualTo(followeeId);
    }

    @Test
    void 자기_자신을_팔로우하면_SELF_FOLLOW_NOT_ALLOWED를_반환한다() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> followService.follow(userId, userId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SELF_FOLLOW_NOT_ALLOWED));

        verify(followRepository, never()).existsByFollowerIdAndFolloweeId(any(), any());
        verify(followRepository, never()).save(any(Follow.class));
    }

    @Test
    void 이미_팔로우_중이면_FOLLOW_ALREADY_EXISTS를_반환한다() {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
                .thenReturn(true);

        assertThatThrownBy(() -> followService.follow(followerId, followeeId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FOLLOW_ALREADY_EXISTS));

        verify(followRepository, never()).save(any(Follow.class));
    }

    @Test
    void 팔로우_취소는_본인_관계를_삭제한다() {
        UUID followId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        Follow follow = follow(followId, followerId, UUID.randomUUID());
        when(followRepository.findById(followId)).thenReturn(Optional.of(follow));

        followService.unfollow(followId, followerId);

        verify(followRepository).delete(follow);
    }

    @Test
    void 존재하지_않는_팔로우_취소는_FOLLOW_NOT_FOUND를_반환한다() {
        UUID followId = UUID.randomUUID();
        when(followRepository.findById(followId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.unfollow(followId, UUID.randomUUID()))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FOLLOW_NOT_FOUND));

        verify(followRepository, never()).delete(any(Follow.class));
    }

    @Test
    void 본인_소유가_아닌_팔로우_취소는_FOLLOW_ACCESS_DENIED를_반환한다() {
        UUID followId = UUID.randomUUID();
        Follow follow = follow(followId, UUID.randomUUID(), UUID.randomUUID());
        when(followRepository.findById(followId)).thenReturn(Optional.of(follow));

        assertThatThrownBy(() -> followService.unfollow(followId, UUID.randomUUID()))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FOLLOW_ACCESS_DENIED));

        verify(followRepository, never()).delete(any(Follow.class));
    }

    @Test
    void 팔로우_여부_조회는_관계가_있으면_반환한다() {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        Follow follow = follow(UUID.randomUUID(), followerId, followeeId);
        when(followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId))
                .thenReturn(Optional.of(follow));

        assertThat(followService.getFollowStatus(followerId, followeeId)).isSameAs(follow);
    }

    @Test
    void 팔로우_관계가_없으면_조회시_FOLLOW_NOT_FOUND를_반환한다() {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        when(followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.getFollowStatus(followerId, followeeId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FOLLOW_NOT_FOUND));
    }

    @Test
    void 팔로워_수는_저장소_집계값을_그대로_반환한다() {
        UUID followeeId = UUID.randomUUID();
        when(followRepository.countByFolloweeId(followeeId)).thenReturn(3L);

        assertThat(followService.countFollowers(followeeId)).isEqualTo(3L);
    }

    private Follow follow(UUID id, UUID followerId, UUID followeeId) {
        return Follow.builder()
                .id(id)
                .followerId(followerId)
                .followeeId(followeeId)
                .build();
    }
}