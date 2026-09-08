package com.codeit.modoo_playlist.core.domain.message.entity;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseEntity;
import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@SuperBuilder
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // 1:1에서는 수신자
    // 실시간 오픈 채팅에서는 NULL 처리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    // 실시간 오픈채팅에서만 사용
    // 일반 DM에서는 null로 사용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MessageType type;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    // 1:1 DM에서만 사용
    // 오픈 채팅에서는 Null
    @Column(name = "read_at")
    private LocalDateTime readAt;

    public void markAsRead() {
        this.readAt = LocalDateTime.now();
    }

    public boolean isRead() {
        return this.readAt != null;
    }
}