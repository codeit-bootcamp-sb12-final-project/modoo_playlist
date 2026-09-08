package com.codeit.modoo_playlist.core.domain.content.entity;

import com.codeit.modoo_playlist.core.domain.content.type.SportsStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "content_sports",
        indexes = @Index(name = "IDX_SPORTS_KICKOFF", columnList = "status,kickoff_at")
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentSports {

    @Id
    @Column(name = "content_id", columnDefinition = "BINARY(16)")
    private UUID contentId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id")
    private Content content;

    @Column(name = "sport_type", nullable = false, length = 50)
    private String sportType;

    @Column(length = 100)
    private String league;

    @Column(length = 20)
    private String season;

    @Column(name = "home_team", length = 100)
    private String homeTeam;

    @Column(name = "away_team", length = 100)
    private String awayTeam;

    @Column(length = 100)
    private String venue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SportsStatus status = SportsStatus.SCHEDULED;

    @Column(name = "kickoff_at", nullable = false)
    private Instant kickoffAt;
}
