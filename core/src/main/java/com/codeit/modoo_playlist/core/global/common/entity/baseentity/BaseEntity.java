package com.codeit.modoo_playlist.core.global.common.entity.baseentity;

import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@ToString
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
public abstract class BaseEntity {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "Binary(16)")
    private UUID id;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = Generators.timeBasedEpochGenerator().generate();
        }
    }

    @CreatedDate
    @Column(columnDefinition = "timestamp with time zone default now()",
            updatable = false, nullable = false)
    private Instant createdAt;
}
