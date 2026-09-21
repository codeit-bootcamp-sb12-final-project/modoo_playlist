package com.codeit.modoo_playlist.moduleapi.domain.notification.service.impl;

import com.codeit.modoo_playlist.core.domain.notification.entity.Notification;
import com.codeit.modoo_playlist.core.domain.notification.entity.NotificationLevel;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.notification.mapper.NotificationMapper;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.NotificationRepository;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationListCondition;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.notification.service.NotificationService;
import com.codeit.modoo_playlist.moduleapi.domain.notification.sse.SseEmitterRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SseEmitterRepository sseEmitterRepository;

    @Override
    @Transactional
    public Notification create(UUID receiverId, String title, String content, NotificationLevel level, UUID sourceId) {

        Notification notification = buildNotification(receiverId, title, content, level, sourceId);

        Notification saved = notificationRepository.save(notification);

        sseEmitterRepository.sendToUser(receiverId, "notifications", notificationMapper.toResponse(saved));

        return saved;
    }

    @Override
    @Transactional
    public List<Notification> createBatch(
            List<UUID> receiverIds, String title, String content, NotificationLevel level, UUID sourceId
    ) {
        if (receiverIds.isEmpty()) {
            return List.of();
        }

        List<Notification> notifications = receiverIds.stream()
                .map(receiverId -> buildNotification(receiverId, title, content, level, sourceId))
                .toList();

        List<Notification> saved = notificationRepository.saveAll(notifications);

        saved.forEach(notification -> sseEmitterRepository.sendToUser(
                notification.getReceiverId(), "notifications", notificationMapper.toResponse(notification)
        ));

        return saved;
    }

    @Override
    public NotificationQueryPage getNotifications(NotificationListCondition condition) {
        return notificationRepository.findAllByCondition(condition);
    }

    @Override
    @Transactional
    public void readNotification(UUID notificationId, UUID requesterId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiverId().equals(requesterId)) {
            throw new BaseException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
    }

    @Override
    @Transactional
    public void readAllNotifications(UUID requesterId) {
        notificationRepository.markAllAsReadByReceiverId(requesterId);
    }

    private Notification buildNotification(
            UUID receiverId, String title, String content, NotificationLevel level, UUID sourceId
    ) {
        return Notification.builder()
                .receiverId(receiverId)
                .title(title)
                .content(content)
                .level(level)
                .sourceId(sourceId)
                .build();
    }

    @Override
    public long countUnread(UUID requesterId) {
        return notificationRepository.countByReceiverIdAndReadFalse(requesterId);
    }

}