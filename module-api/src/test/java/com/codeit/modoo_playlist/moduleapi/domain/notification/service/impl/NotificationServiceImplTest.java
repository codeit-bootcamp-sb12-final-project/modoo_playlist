package com.codeit.modoo_playlist.moduleapi.domain.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.notification.entity.Notification;
import com.codeit.modoo_playlist.core.domain.notification.entity.NotificationLevel;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.notification.mapper.NotificationMapper;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.NotificationRepository;
import com.codeit.modoo_playlist.moduleapi.domain.notification.sse.SseEmitterRepository;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationListCondition;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationQueryPage;
import com.codeit.modoo_playlist.moduleapi.dto.notification.response.NotificationResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private static final NotificationLevel ANY_LEVEL = NotificationLevel.values()[0];

    private Notification buildNotification(UUID receiverId) {
        return Notification.builder()
                .receiverId(receiverId)
                .title("title")
                .content("content")
                .level(ANY_LEVEL)
                .sourceId(UUID.randomUUID())
                .build();
    }

    @Test
    void 본인_알림을_읽음처리하면_markAsRead가_반영된다() {
        UUID receiverId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = buildNotification(receiverId);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.readNotification(notificationId, receiverId);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void 존재하지_않는_알림을_읽음처리하면_NOTIFICATION_NOT_FOUND_예외가_발생한다() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.readNotification(notificationId, UUID.randomUUID()))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void 남의_알림을_읽음처리하려_하면_NOTIFICATION_ACCESS_DENIED_예외가_발생하고_상태는_바뀌지_않는다() {
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = buildNotification(ownerId);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.readNotification(notificationId, requesterId))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_ACCESS_DENIED);

        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void readAllNotifications는_레포지토리의_벌크_업데이트를_그대로_위임한다() {
        UUID requesterId = UUID.randomUUID();

        notificationService.readAllNotifications(requesterId);

        verify(notificationRepository).markAllAsReadByReceiverId(requesterId);
    }

    @Test
    void countUnread는_레포지토리_카운트_결과를_그대로_반환한다() {
        UUID requesterId = UUID.randomUUID();
        when(notificationRepository.countByReceiverIdAndReadFalse(requesterId)).thenReturn(3L);

        long result = notificationService.countUnread(requesterId);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void createBatch는_수신자_목록이_비어있으면_아무것도_저장하지_않는다() {
        List<Notification> result = notificationService.createBatch(
                List.of(), "title", "content", ANY_LEVEL, UUID.randomUUID()
        );

        assertThat(result).isEmpty();
        verifyNoInteractions(notificationRepository, notificationMapper, sseEmitterRepository);
    }

    @Test
    void createBatch는_saveAll로_한번에_저장하고_수신자별로_SSE를_전송한다() {
        UUID receiverId1 = UUID.randomUUID();
        UUID receiverId2 = UUID.randomUUID();
        List<UUID> receiverIds = List.of(receiverId1, receiverId2);
        UUID sourceId = UUID.randomUUID();

        List<Notification> saved = receiverIds.stream()
                .map(this::buildNotification)
                .toList();

        NotificationResponse dummyResponse =
                new NotificationResponse(UUID.randomUUID(), Instant.now(), receiverId1, "title", "content", ANY_LEVEL, false);

        when(notificationRepository.saveAll(anyList())).thenReturn(saved);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(dummyResponse);

        List<Notification> result = notificationService.createBatch(
                receiverIds, "title", "content", ANY_LEVEL, sourceId
        );

        assertThat(result).hasSize(2);
        verify(notificationRepository, times(1)).saveAll(anyList());
        verify(sseEmitterRepository, times(1)).sendToUser(eq(receiverId1), eq("notifications"), any());
        verify(sseEmitterRepository, times(1)).sendToUser(eq(receiverId2), eq("notifications"), any());
    }

    @Test
    void create는_알림을_저장하고_수신자에게_SSE로_전송한다() {
        UUID receiverId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        Notification saved = buildNotification(receiverId);

        NotificationResponse dummyResponse =
                new NotificationResponse(UUID.randomUUID(), Instant.now(), receiverId, "title", "content", ANY_LEVEL, false);

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
        when(notificationMapper.toResponse(saved)).thenReturn(dummyResponse);

        Notification result = notificationService.create(receiverId, "title", "content", ANY_LEVEL, sourceId);

        assertThat(result).isEqualTo(saved);
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(sseEmitterRepository, times(1)).sendToUser(eq(receiverId), eq("notifications"), any());
    }

    @Test
    void getNotifications는_레포지토리_조회_결과를_그대로_반환한다() {
        NotificationListCondition condition = mock(NotificationListCondition.class);
        NotificationQueryPage expected = mock(NotificationQueryPage.class);

        when(notificationRepository.findAllByCondition(condition)).thenReturn(expected);

        NotificationQueryPage result = notificationService.getNotifications(condition);

        assertThat(result).isEqualTo(expected);
        verify(notificationRepository, times(1)).findAllByCondition(condition);
        verifyNoInteractions(notificationMapper, sseEmitterRepository);
    }
}