package com.codeit.modoo_playlist.core.global.common.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.uuid.Generators;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "binary_content")
@Getter @Setter @SuperBuilder @ToString
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BinaryContent {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = Generators.timeBasedEpochGenerator().generate();
        }
    }

    @Column(nullable = false, length = 255)
    private String originFileName;

    @Column(nullable = false, length = 255)
    private String renamedFileName;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, length = 100)
    private String contentType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamp with time zone default now()")
    private Instant createdAt;
}
