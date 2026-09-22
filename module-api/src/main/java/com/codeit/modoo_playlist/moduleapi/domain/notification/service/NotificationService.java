package com.codeit.modoo_playlist.moduleapi.domain.notification.service;

import com.codeit.modoo_playlist.core.domain.notification.entity.Notification;
import com.codeit.modoo_playlist.core.domain.notification.entity.NotificationLevel;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationListCondition;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationQueryPage;
import java.util.List;
import java.util.UUID;

public interface NotificationService {

    Notification create(UUID receiverId, String title, String content, NotificationLevel level, UUID sourceId);

    List<Notification> createBatch(List<UUID> receiverIds, String title, String content, NotificationLevel level, UUID sourceId);

    NotificationQueryPage getNotifications(NotificationListCondition condition);

    void readNotification(UUID notificationId, UUID requesterId);

    void readAllNotifications(UUID requesterId);

    long countUnread(UUID requesterId);

}