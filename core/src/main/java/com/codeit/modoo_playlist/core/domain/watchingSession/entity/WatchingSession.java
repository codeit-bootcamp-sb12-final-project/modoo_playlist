package com.codeit.modoo_playlist.core.domain.watchingSession.entity;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "watching_sessions")
@Getter
@SuperBuilder
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchingSession extends BaseUpdatableEntity {

    // 시청중인 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watcher_id", nullable = false)
    private User watcher;

    // 시청중인 콘텐츠
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "watched_seconds")
    private Integer watchedSeconds;

    public void end() {
        this.endedAt = LocalDateTime.now();

        if (this.startedAt != null) {
            this.watchedSeconds = Math.toIntExact(
                    Duration.between(this.startedAt, this.endedAt).getSeconds()
            );
        }
    }

    public boolean isWatching() {
        return this.endedAt == null;
    }
}
