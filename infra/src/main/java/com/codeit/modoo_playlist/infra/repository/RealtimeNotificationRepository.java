package com.codeit.modoo_playlist.infra.repository;

import com.codeit.modoo_playlist.core.domain.notification.entity.Notification;
import com.codeit.modoo_playlist.core.domain.notification.entity.NotificationLevel;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface RealtimeNotificationRepository extends Repository<Notification, UUID> {
    Notification save(Notification notification);


}
