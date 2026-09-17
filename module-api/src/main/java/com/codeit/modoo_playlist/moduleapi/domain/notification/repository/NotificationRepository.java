package com.codeit.modoo_playlist.moduleapi.domain.notification.repository;

import com.codeit.modoo_playlist.core.domain.notification.entity.Notification;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationQueryRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationQueryRepository {
}
