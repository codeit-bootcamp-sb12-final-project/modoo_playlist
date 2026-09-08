package com.codeit.modoo_playlist.core.domain.content.entity;

import com.codeit.modoo_playlist.core.domain.content.type.VideoReleaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "content_videos")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentVideo {

    @Id
    @Column(name = "content_id", columnDefinition = "BINARY(16)")
    private UUID contentId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    @Column(name = "runtime_minutes")
    private Integer runtimeMinutes;

    @Column(name = "collection_name", length = 100)
    private String collectionName;

    @Column(name = "imdb_id", length = 20)
    private String imdbId;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_status", length = 30)
    private VideoReleaseStatus releaseStatus;

    @Column(name = "number_of_seasons")
    private Integer numberOfSeasons;

    @Column(name = "number_of_episodes")
    private Integer numberOfEpisodes;

    @Column(name = "original_language", length = 10)
    private String originalLanguage;

    private Float popularity;

    @Column(name = "external_rating", precision = 3, scale = 1)
    private BigDecimal externalRating;

    @Column(name = "external_rating_count")
    private Integer externalRatingCount;
}
