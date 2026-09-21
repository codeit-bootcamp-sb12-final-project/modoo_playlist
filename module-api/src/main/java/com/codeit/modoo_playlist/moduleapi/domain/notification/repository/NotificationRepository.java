package com.codeit.modoo_playlist.moduleapi.domain.notification.repository;

import com.codeit.modoo_playlist.core.domain.notification.entity.Notification;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationQueryRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationQueryRepository {

    long countByReceiverIdAndReadFalse(UUID receiverId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.receiverId = :receiverId AND n.read = false")
    int markAllAsReadByReceiverId(@Param("receiverId") UUID receiverId);

}