package com.codeit.modoo_playlist.moduleapi.domain.playlist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
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
import com.codeit.modoo_playlist.core.domain.playlist.entity.GeneratedBy;
import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistContent;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistContentId;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistSubscription;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistSubscriptionId;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.mapper.ContentMapper;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.mapper.PlaylistMapper;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistSubscriptionRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query.PlaylistListCondition;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query.PlaylistQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentSummaryResponse;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.request.PlaylistListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.response.PlaylistCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.response.PlaylistResponse;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceImplTest {

    @Mock private PlaylistRepository playlistRepository;
    @Mock private PlaylistContentRepository playlistContentRepository;
    @Mock private PlaylistSubscriptionRepository playlistSubscriptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private ContentTagRepository contentTagRepository;
    @Mock private PlaylistMapper playlistMapper;
    @Mock private ContentMapper contentMapper;
    @InjectMocks private PlaylistServiceImpl playlistService;

    @Test
    void 플레이리스트를_생성하면_저장된다() {
        UUID ownerId = UUID.randomUUID();
        UUID playlistId = UUID.randomUUID();
        when(playlistRepository.save(any(Playlist.class)))
                .thenReturn(playlist(playlistId, ownerId, "제목", "설명"));

        UUID result = playlistService.createPlaylist(ownerId, "제목", "설명");

        assertThat(result).isEqualTo(playlistId);
        ArgumentCaptor<Playlist> captor = ArgumentCaptor.forClass(Playlist.class);
        verify(playlistRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(ownerId);
        assertThat(captor.getValue().getTitle()).isEqualTo("제목");
        assertThat(captor.getValue().getDescription()).isEqualTo("설명");
        assertThat(captor.getValue().getGeneratedBy()).isEqualTo(GeneratedBy.USER);
    }

    @Test
    void 플레이리스트를_수정하면_내용이_변경되고_응답을_반환한다() {
        UUID playlistId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, ownerId, "이전 제목", "이전 설명");
        User owner = user(ownerId, "jhdb");
        UserSummaryResponse ownerSummary =
                new UserSummaryResponse(owner.getId(), owner.getUsername(), owner.getProfileImageUrl());
        PlaylistResponse expected =
                new PlaylistResponse(playlistId, ownerSummary, "새 제목", "새 설명", null, 0L, false, List.of());

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(playlistSubscriptionRepository.countById_PlaylistId(playlistId)).thenReturn(0L);
        when(playlistSubscriptionRepository.existsById(new PlaylistSubscriptionId(playlistId, ownerId))).thenReturn(false);
        when(playlistContentRepository.findAllById_PlaylistIdOrderByCreatedAtAsc(playlistId)).thenReturn(List.of());
        when(contentRepository.findAllById(List.of())).thenReturn(List.of());
        when(playlistMapper.toResponse(playlist, ownerSummary, 0L, false, List.of())).thenReturn(expected);

        PlaylistResponse result = playlistService.updatePlaylist(playlistId, ownerId, "새 제목", "새 설명");

        assertThat(result).isEqualTo(expected);
        assertThat(playlist.getTitle()).isEqualTo("새 제목");
        assertThat(playlist.getDescription()).isEqualTo("새 설명");
    }

    @Test
    void 존재하지_않는_플레이리스트를_수정하면_PLAYLIST_NOT_FOUND를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.updatePlaylist(playlistId, UUID.randomUUID(), "제목", "설명"))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND));
    }

    @Test
    void 본인_소유가_아닌_플레이리스트를_수정하면_PLAYLIST_ACCESS_DENIED를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, UUID.randomUUID(), "제목", "설명");
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.updatePlaylist(playlistId, UUID.randomUUID(), "새 제목", "새 설명"))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_ACCESS_DENIED));
        assertThat(playlist.getTitle()).isEqualTo("제목");
    }

    @Test
    void 플레이리스트를_삭제하면_저장소에서_삭제된다() {
        UUID playlistId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, ownerId, "제목", "설명");
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        playlistService.deletePlaylist(playlistId, ownerId);

        verify(playlistRepository).delete(playlist);
    }

    @Test
    void 존재하지_않는_플레이리스트를_삭제하면_PLAYLIST_NOT_FOUND를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.deletePlaylist(playlistId, UUID.randomUUID()))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND));
        verify(playlistRepository, never()).delete(any(Playlist.class));
    }

    @Test
    void 본인_소유가_아닌_플레이리스트를_삭제하면_PLAYLIST_ACCESS_DENIED를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, UUID.randomUUID(), "제목", "설명");
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.deletePlaylist(playlistId, UUID.randomUUID()))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_ACCESS_DENIED));
        verify(playlistRepository, never()).delete(any(Playlist.class));
    }

    @Test
    void 플레이리스트_단건_조회는_존재하면_반환한다() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, UUID.randomUUID(), "제목", "설명");
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThat(playlistService.getPlaylist(playlistId)).isSameAs(playlist);
    }

    @Test
    void 플레이리스트_단건_조회는_존재하지_않으면_PLAYLIST_NOT_FOUND를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.getPlaylist(playlistId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND));
    }

    @Test
    void 콘텐츠를_추가하면_저장된다() {
        UUID playlistId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, ownerId, "제목", "설명");
        PlaylistContentId id = new PlaylistContentId(playlistId, contentId);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(playlistContentRepository.existsById(id)).thenReturn(false);

        playlistService.addContent(playlistId, ownerId, contentId);

        ArgumentCaptor<PlaylistContent> captor = ArgumentCaptor.forClass(PlaylistContent.class);
        verify(playlistContentRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
    }

    @Test
    void 이미_추가된_콘텐츠를_다시_추가하면_PLAYLIST_CONTENT_ALREADY_EXISTS를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, ownerId, "제목", "설명");
        PlaylistContentId id = new PlaylistContentId(playlistId, contentId);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(playlistContentRepository.existsById(id)).thenReturn(true);

        assertThatThrownBy(() -> playlistService.addContent(playlistId, ownerId, contentId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS));
        verify(playlistContentRepository, never()).save(any(PlaylistContent.class));
    }

    @Test
    void 본인_소유가_아닌_플레이리스트에_콘텐츠를_추가하면_PLAYLIST_ACCESS_DENIED를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, UUID.randomUUID(), "제목", "설명");
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.addContent(playlistId, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_ACCESS_DENIED));
        verify(playlistContentRepository, never()).existsById(any(PlaylistContentId.class));
    }

    @Test
    void 콘텐츠를_제거하면_삭제된다() {
        UUID playlistId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, ownerId, "제목", "설명");
        PlaylistContentId id = new PlaylistContentId(playlistId, contentId);
        PlaylistContent playlistContent = PlaylistContent.builder().id(id).build();
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(playlistContentRepository.findById(id)).thenReturn(Optional.of(playlistContent));

        playlistService.removeContent(playlistId, ownerId, contentId);

        verify(playlistContentRepository).delete(playlistContent);
    }

    @Test
    void 존재하지_않는_콘텐츠를_제거하면_PLAYLIST_CONTENT_NOT_FOUND를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, ownerId, "제목", "설명");
        PlaylistContentId id = new PlaylistContentId(playlistId, contentId);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(playlistContentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.removeContent(playlistId, ownerId, contentId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_CONTENT_NOT_FOUND));
        verify(playlistContentRepository, never()).delete(any(PlaylistContent.class));
    }

    @Test
    void 구독하면_저장된다() {
        UUID playlistId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, subscriberId);
        when(playlistRepository.existsById(playlistId)).thenReturn(true);
        when(playlistSubscriptionRepository.existsById(id)).thenReturn(false);

        playlistService.subscribe(playlistId, subscriberId);

        ArgumentCaptor<PlaylistSubscription> captor = ArgumentCaptor.forClass(PlaylistSubscription.class);
        verify(playlistSubscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
    }

    @Test
    void 존재하지_않는_플레이리스트를_구독하면_PLAYLIST_NOT_FOUND를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        when(playlistRepository.existsById(playlistId)).thenReturn(false);

        assertThatThrownBy(() -> playlistService.subscribe(playlistId, UUID.randomUUID()))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND));
        verify(playlistSubscriptionRepository, never()).save(any(PlaylistSubscription.class));
    }

    @Test
    void 이미_구독중이면_PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, subscriberId);
        when(playlistRepository.existsById(playlistId)).thenReturn(true);
        when(playlistSubscriptionRepository.existsById(id)).thenReturn(true);

        assertThatThrownBy(() -> playlistService.subscribe(playlistId, subscriberId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS));
        verify(playlistSubscriptionRepository, never()).save(any(PlaylistSubscription.class));
    }

    @Test
    void 구독을_취소하면_삭제된다() {
        UUID playlistId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, subscriberId);
        PlaylistSubscription subscription = PlaylistSubscription.builder().id(id).build();
        when(playlistSubscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));

        playlistService.unsubscribe(playlistId, subscriberId);

        verify(playlistSubscriptionRepository).delete(subscription);
    }

    @Test
    void 구독하지_않은_상태에서_취소하면_PLAYLIST_SUBSCRIPTION_NOT_FOUND를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, subscriberId);
        when(playlistSubscriptionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.unsubscribe(playlistId, subscriberId))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND));
        verify(playlistSubscriptionRepository, never()).delete(any(PlaylistSubscription.class));
    }

    @Test
    void 구독_여부_조회는_저장소_결과를_그대로_반환한다() {
        UUID playlistId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, subscriberId);
        when(playlistSubscriptionRepository.existsById(id)).thenReturn(true);

        assertThat(playlistService.isSubscribedByMe(playlistId, subscriberId)).isTrue();
    }

    @Test
    void 구독자_수는_저장소_집계값을_그대로_반환한다() {
        UUID playlistId = UUID.randomUUID();
        when(playlistSubscriptionRepository.countById_PlaylistId(playlistId)).thenReturn(5L);

        assertThat(playlistService.countSubscribers(playlistId)).isEqualTo(5L);
    }

    @Test
    void 플레이리스트_응답_조회는_구독_여부와_구독자_수를_포함한다() {
        UUID playlistId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, ownerId, "제목", "설명");
        User owner = user(ownerId, "owner");
        UserSummaryResponse ownerSummary =
                new UserSummaryResponse(owner.getId(), owner.getUsername(), owner.getProfileImageUrl());
        PlaylistResponse expected =
                new PlaylistResponse(playlistId, ownerSummary, "제목", "설명", null, 3L, true, List.of());

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(playlistSubscriptionRepository.countById_PlaylistId(playlistId)).thenReturn(3L);
        when(playlistSubscriptionRepository.existsById(new PlaylistSubscriptionId(playlistId, viewerId))).thenReturn(true);
        when(playlistContentRepository.findAllById_PlaylistIdOrderByCreatedAtAsc(playlistId)).thenReturn(List.of());
        when(contentRepository.findAllById(List.of())).thenReturn(List.of());
        when(playlistMapper.toResponse(playlist, ownerSummary, 3L, true, List.of())).thenReturn(expected);

        assertThat(playlistService.getPlaylistResponse(playlistId, viewerId)).isEqualTo(expected);
    }

    @Test
    void 플레이리스트_응답_조회는_viewerId가_없으면_구독_여부를_false로_처리한다() {
        UUID playlistId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, ownerId, "제목", "설명");
        User owner = user(ownerId, "owner");
        UserSummaryResponse ownerSummary =
                new UserSummaryResponse(owner.getId(), owner.getUsername(), owner.getProfileImageUrl());
        PlaylistResponse expected =
                new PlaylistResponse(playlistId, ownerSummary, "제목", "설명", null, 0L, false, List.of());

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(playlistSubscriptionRepository.countById_PlaylistId(playlistId)).thenReturn(0L);
        when(playlistContentRepository.findAllById_PlaylistIdOrderByCreatedAtAsc(playlistId)).thenReturn(List.of());
        when(contentRepository.findAllById(List.of())).thenReturn(List.of());
        when(playlistMapper.toResponse(playlist, ownerSummary, 0L, false, List.of())).thenReturn(expected);

        assertThat(playlistService.getPlaylistResponse(playlistId, null)).isEqualTo(expected);
        verify(playlistSubscriptionRepository, never()).existsById(any(PlaylistSubscriptionId.class));
    }

    @Test
    void 플레이리스트_응답_조회는_소유자가_없으면_USER_NOT_FOUND를_반환한다() {
        UUID playlistId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, ownerId, "제목", "설명");
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.getPlaylistResponse(playlistId, null))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void 플레이리스트_응답_조회는_담긴_콘텐츠_목록을_포함한다() {
        UUID playlistId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID contentId1 = UUID.randomUUID();
        UUID contentId2 = UUID.randomUUID();
        Playlist playlist = playlist(playlistId, ownerId, "제목", "설명");
        User owner = user(ownerId, "owner");
        UserSummaryResponse ownerSummary =
                new UserSummaryResponse(owner.getId(), owner.getUsername(), owner.getProfileImageUrl());
        PlaylistContent playlistContent1 =
                PlaylistContent.builder().id(new PlaylistContentId(playlistId, contentId1)).build();
        PlaylistContent playlistContent2 =
                PlaylistContent.builder().id(new PlaylistContentId(playlistId, contentId2)).build();
        Content content1 = content(contentId1, "타이틀1");
        Content content2 = content(contentId2, "타이틀2");
        ContentSummaryResponse summary1 =
                new ContentSummaryResponse(contentId1, "movie", "타이틀1", null, null, List.of(), BigDecimal.ZERO, 0);
        ContentSummaryResponse summary2 =
                new ContentSummaryResponse(contentId2, "movie", "타이틀2", null, null, List.of(), BigDecimal.ZERO, 0);
        PlaylistResponse expected = new PlaylistResponse(
                playlistId, ownerSummary, "제목", "설명", null, 0L, false, List.of(summary1, summary2));

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(playlistSubscriptionRepository.countById_PlaylistId(playlistId)).thenReturn(0L);
        when(playlistContentRepository.findAllById_PlaylistIdOrderByCreatedAtAsc(playlistId))
                .thenReturn(List.of(playlistContent1, playlistContent2));
        when(contentRepository.findAllById(List.of(contentId1, contentId2))).thenReturn(List.of(content1, content2));
        when(contentTagRepository.findAllWithTagByContentIds(List.of(contentId1, contentId2))).thenReturn(List.of());
        when(contentMapper.toSummary(content1, List.of())).thenReturn(summary1);
        when(contentMapper.toSummary(content2, List.of())).thenReturn(summary2);
        when(playlistMapper.toResponse(playlist, ownerSummary, 0L, false, List.of(summary1, summary2)))
                .thenReturn(expected);

        assertThat(playlistService.getPlaylistResponse(playlistId, null)).isEqualTo(expected);
    }

    @Test
    void 플레이리스트_목록_조회_결과가_없으면_빈_커서_응답을_반환한다() {
        PlaylistListRequest request = new PlaylistListRequest(null, null, null, null, null, 20, "DESCENDING", "createdAt");
        PlaylistQueryPage page = new PlaylistQueryPage(List.of(), null, null, false, 0L);
        PlaylistCursorResponse expected =
                new PlaylistCursorResponse(List.of(), null, null, false, 0L, "createdAt", "DESCENDING");
        when(playlistRepository.findAllByCondition(any(PlaylistListCondition.class))).thenReturn(page);
        when(playlistMapper.toCursorResponse(page, List.of(), "createdAt", "DESCENDING")).thenReturn(expected);

        PlaylistCursorResponse result = playlistService.getPlaylists(request, null);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<PlaylistListCondition> captor = ArgumentCaptor.forClass(PlaylistListCondition.class);
        verify(playlistRepository).findAllByCondition(captor.capture());
        assertThat(captor.getValue().sortBy()).isEqualTo(PlaylistListCondition.SortType.CREATED_AT);
        assertThat(captor.getValue().sortDirection()).isEqualTo(PlaylistListCondition.SortDirection.DESCENDING);
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void 플레이리스트_목록_조회는_구독자_수와_소유자_정보를_배치로_조회해_포함한다() {
        UUID viewerId = UUID.randomUUID();
        UUID ownerId1 = UUID.randomUUID();
        UUID ownerId2 = UUID.randomUUID();
        UUID playlistId1 = UUID.randomUUID();
        UUID playlistId2 = UUID.randomUUID();
        Playlist playlist1 = playlist(playlistId1, ownerId1, "제목1", "설명1");
        Playlist playlist2 = playlist(playlistId2, ownerId2, "제목2", "설명2");
        User owner1 = user(ownerId1, "owner1");
        UserSummaryResponse ownerSummary1 =
                new UserSummaryResponse(owner1.getId(), owner1.getUsername(), owner1.getProfileImageUrl());
        PlaylistSubscription subscription1 = PlaylistSubscription.builder()
                .id(new PlaylistSubscriptionId(playlistId1, viewerId))
                .build();
        PlaylistSubscription subscription2 = PlaylistSubscription.builder()
                .id(new PlaylistSubscriptionId(playlistId2, UUID.randomUUID()))
                .build();
        PlaylistResponse response1 =
                new PlaylistResponse(playlistId1, ownerSummary1, "제목1", "설명1", null, 1L, true, List.of());
        PlaylistResponse response2 =
                new PlaylistResponse(playlistId2, null, "제목2", "설명2", null, 1L, false, List.of());
        PlaylistListRequest request = new PlaylistListRequest(null, null, null, null, null, 20, "DESCENDING", "createdAt");
        PlaylistQueryPage page = new PlaylistQueryPage(List.of(playlist1, playlist2), "cursor", playlistId2, true, 2L);
        PlaylistCursorResponse expected = new PlaylistCursorResponse(
                List.of(response1, response2), "cursor", playlistId2, true, 2L, "createdAt", "DESCENDING");

        when(playlistRepository.findAllByCondition(any(PlaylistListCondition.class))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of(owner1));
        when(playlistSubscriptionRepository.findAllById_PlaylistIdIn(List.of(playlistId1, playlistId2)))
                .thenReturn(List.of(subscription1, subscription2));
        when(playlistContentRepository.findAllById_PlaylistIdInOrderByCreatedAtAsc(List.of(playlistId1, playlistId2)))
                .thenReturn(List.of());
        when(playlistMapper.toResponse(playlist1, ownerSummary1, 1L, true, List.of())).thenReturn(response1);
        when(playlistMapper.toResponse(playlist2, null, 1L, false, List.of())).thenReturn(response2);
        when(playlistMapper.toCursorResponse(page, List.of(response1, response2), "createdAt", "DESCENDING"))
                .thenReturn(expected);

        PlaylistCursorResponse result = playlistService.getPlaylists(request, viewerId);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void 플레이리스트_목록_조회는_각_플레이리스트의_콘텐츠_목록을_포함한다() {
        UUID ownerId = UUID.randomUUID();
        UUID playlistId1 = UUID.randomUUID();
        UUID playlistId2 = UUID.randomUUID();
        UUID contentId1 = UUID.randomUUID();
        UUID contentId2 = UUID.randomUUID();
        Playlist playlist1 = playlist(playlistId1, ownerId, "제목1", "설명1");
        Playlist playlist2 = playlist(playlistId2, ownerId, "제목2", "설명2");
        User owner = user(ownerId, "owner");
        UserSummaryResponse ownerSummary =
                new UserSummaryResponse(owner.getId(), owner.getUsername(), owner.getProfileImageUrl());
        PlaylistContent playlistContent1 =
                PlaylistContent.builder().id(new PlaylistContentId(playlistId1, contentId1)).build();
        PlaylistContent playlistContent2 =
                PlaylistContent.builder().id(new PlaylistContentId(playlistId2, contentId2)).build();
        Content content1 = content(contentId1, "타이틀1");
        Content content2 = content(contentId2, "타이틀2");
        ContentSummaryResponse summary1 =
                new ContentSummaryResponse(contentId1, "movie", "타이틀1", null, null, List.of(), BigDecimal.ZERO, 0);
        ContentSummaryResponse summary2 =
                new ContentSummaryResponse(contentId2, "movie", "타이틀2", null, null, List.of(), BigDecimal.ZERO, 0);
        PlaylistResponse response1 =
                new PlaylistResponse(playlistId1, ownerSummary, "제목1", "설명1", null, 0L, false, List.of(summary1));
        PlaylistResponse response2 =
                new PlaylistResponse(playlistId2, ownerSummary, "제목2", "설명2", null, 0L, false, List.of(summary2));
        PlaylistListRequest request = new PlaylistListRequest(null, null, null, null, null, 20, "DESCENDING", "createdAt");
        PlaylistQueryPage page = new PlaylistQueryPage(List.of(playlist1, playlist2), null, null, false, 2L);
        PlaylistCursorResponse expected = new PlaylistCursorResponse(
                List.of(response1, response2), null, null, false, 2L, "createdAt", "DESCENDING");

        when(playlistRepository.findAllByCondition(any(PlaylistListCondition.class))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));
        when(playlistSubscriptionRepository.findAllById_PlaylistIdIn(List.of(playlistId1, playlistId2)))
                .thenReturn(List.of());
        when(playlistContentRepository.findAllById_PlaylistIdInOrderByCreatedAtAsc(List.of(playlistId1, playlistId2)))
                .thenReturn(List.of(playlistContent1, playlistContent2));
        when(contentRepository.findAllById(List.of(contentId1, contentId2))).thenReturn(List.of(content1, content2));
        when(contentTagRepository.findAllWithTagByContentIds(List.of(contentId1, contentId2))).thenReturn(List.of());
        when(contentMapper.toSummary(content1, List.of())).thenReturn(summary1);
        when(contentMapper.toSummary(content2, List.of())).thenReturn(summary2);
        when(playlistMapper.toResponse(playlist1, ownerSummary, 0L, false, List.of(summary1))).thenReturn(response1);
        when(playlistMapper.toResponse(playlist2, ownerSummary, 0L, false, List.of(summary2))).thenReturn(response2);
        when(playlistMapper.toCursorResponse(page, List.of(response1, response2), "createdAt", "DESCENDING"))
                .thenReturn(expected);

        PlaylistCursorResponse result = playlistService.getPlaylists(request, null);

        assertThat(result).isEqualTo(expected);
    }

    private Playlist playlist(UUID id, UUID ownerId, String title, String description) {
        return Playlist.builder()
                .id(id)
                .ownerId(ownerId)
                .title(title)
                .description(description)
                .generatedBy(GeneratedBy.USER)
                .build();
    }

    private User user(UUID id, String username) {
        User user = User.create(username + "@example.com", username, "encoded-password");
        user.updateProfile(username, "https://example.com/" + username + ".png");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Content content(UUID id, String title) {
        return Content.builder()
                .id(id)
                .title(title)
                .build();
    }
}