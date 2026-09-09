package com.codeit.modoo_playlist.core.domain.content.entity;

import com.codeit.modoo_playlist.core.domain.content.type.ContentSource;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "contents",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_CONTENTS_SOURCE_SOURCE_ID",
                columnNames = {"source", "source_id"}
        ),
        indexes = {
                @Index(name = "IDX_CONTENTS_LIVE_TYPE", columnList = "deleted_at,type"),
                @Index(name = "IDX_CONTENTS_RELEASE", columnList = "release_date")
        }
)
@Getter
@SuperBuilder
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseUpdatableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ContentSource source;

    @Column(name = "source_id", length = 100)
    private String sourceId;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "origin_country", length = 20)
    private String originCountry;

    @Column(name = "average_rating", nullable = false, precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "rating_sum", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal ratingSum = BigDecimal.ZERO;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void update(String title, String description, String thumbnailUrl) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (thumbnailUrl != null) {
            this.thumbnailUrl = thumbnailUrl;
        }
    }

    public void softDelete() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }
}
