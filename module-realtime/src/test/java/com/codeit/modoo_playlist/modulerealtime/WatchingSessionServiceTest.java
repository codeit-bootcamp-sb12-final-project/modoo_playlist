package com.codeit.modoo_playlist.modulerealtime;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;
import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
import com.codeit.modoo_playlist.core.domain.watchingSession.dto.WatchingSessionDto;
import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.infra.mapper.ContentSummaryMapper;
import com.codeit.modoo_playlist.infra.repository.RealtimeContentRepository;
import com.codeit.modoo_playlist.infra.repository.RealtimeContentTagRepository;
import com.codeit.modoo_playlist.infra.repository.RealtimeUserRepository;
import com.codeit.modoo_playlist.infra.repository.watchingsession.WatchingSessionRepository;
import com.codeit.modoo_playlist.core.domain.content.dto.ContentSummaryResponse;
import com.codeit.modoo_playlist.infra.mapper.WatchingSessionMapper;
import com.codeit.modoo_playlist.modulerealtime.dto.watchingsession.ChangeType;
import com.codeit.modoo_playlist.modulerealtime.watchingSession.service.WatchingSessionCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchingSessionServiceTest {
    @Mock WatchingSessionRepository apiWatchingSessionRepository;
    @Mock RealtimeUserRepository userRepository;
    @Mock RealtimeContentRepository contentRepository;
    @Mock RealtimeContentTagRepository contentTagRepository;
    @Mock WatchingSessionMapper watchingSessionMapper;
    @Mock ContentSummaryMapper contentMapper;
    @InjectMocks WatchingSessionCommandService service;

    @Test
    void expiresSessionsWithOneTagLookupAcrossDistinctContents() {
        Instant cutoff = Instant.parse("2026-09-16T00:00:00Z");
        Instant lastSeen = cutoff.minusSeconds(60);
        Content first = Content.builder().id(UUID.randomUUID()).build();
        Content second = Content.builder().id(UUID.randomUUID()).build();
        Content untagged = Content.builder().id(UUID.randomUUID()).build();
        List<WatchingSession> sessions = List.of(first, first, second, untagged).stream()
                .<WatchingSession>map(content -> WatchingSession.builder().id(UUID.randomUUID())
                        .content(content).updatedAt(lastSeen).build())
                .toList();
        when(apiWatchingSessionRepository.findByEndedAtIsNullAndUpdatedAtBefore(cutoff)).thenReturn(sessions);
        when(contentTagRepository.findAllWithTagByContentIds(List.of(first.getId(), second.getId(), untagged.getId())))
                .thenReturn(List.of(link(first, "drama"), link(second, "action"), link(first, "comedy")));
        when(contentMapper.toSummary(any(), anyList())).thenAnswer(invocation -> {
            Content content = invocation.getArgument(0);
            return new ContentSummaryResponse(content.getId(), null, null, null, null,
                    invocation.getArgument(1), null, 0);
        });
        when(watchingSessionMapper.toDto(any(), any())).thenAnswer(invocation -> {
            WatchingSession session = invocation.getArgument(0);
            return new WatchingSessionDto(session.getId(), null, null, invocation.getArgument(1));
        });
        when(apiWatchingSessionRepository.countDistinctByContent_IdAndEndedAtIsNull(first.getId()))
                .thenReturn(1L, 0L);

        var changes = service.expireStaleSessions(cutoff);

        assertThat(changes).hasSize(4);
        assertThat(changes).allSatisfy(change -> assertThat(change.type()).isEqualTo(ChangeType.LEAVE));
        assertThat(changes.stream().map(change -> change.watchingSession().content().tags()).toList())
                .containsExactly(List.of("drama", "comedy"), List.of("drama", "comedy"), List.of("action"), List.of());
        assertThat(changes.stream().map(change -> change.watcherCount()).toList()).containsExactly(1L, 0L, 0L, 0L);
        assertThat(sessions).allSatisfy(session -> assertThat(session.getEndedAt())
                .isEqualTo(LocalDateTime.ofInstant(lastSeen, ZoneId.systemDefault())));
        verify(contentTagRepository).findAllWithTagByContentIds(List.of(first.getId(), second.getId(), untagged.getId()));
        verifyNoMoreInteractions(contentTagRepository);
    }

    @Test
    void skipsTagLookupWhenNoSessionsAreStale() {
        Instant cutoff = Instant.now();
        when(apiWatchingSessionRepository.findByEndedAtIsNullAndUpdatedAtBefore(cutoff)).thenReturn(List.of());
        assertThat(service.expireStaleSessions(cutoff)).isEmpty();
        verifyNoInteractions(contentTagRepository, contentMapper, watchingSessionMapper);
    }

    private ContentTag link(Content content, String name) {
        Tag tag = Tag.builder().id(UUID.randomUUID()).name(name).build();
        return ContentTag.builder().id(new ContentTagId(content.getId(), tag.getId()))
                .content(content).tag(tag).build();
    }
}
