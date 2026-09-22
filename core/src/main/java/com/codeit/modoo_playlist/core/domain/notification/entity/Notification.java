package com.codeit.modoo_playlist.core.domain.notification.entity;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(name = "receiver_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID receiverId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationLevel level;

    @Column(name = "source_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID sourceId;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    public void markAsRead() {
        this.read = true;
    }

}