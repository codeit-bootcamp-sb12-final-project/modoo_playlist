package com.codeit.modoo_playlist.core.domain.content.entity;

import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "content_people",
        indexes = {
                @Index(name = "IDX_PEOPLE_CONTENT", columnList = "content_id,display_order"),
                @Index(name = "IDX_PEOPLE_NAME", columnList = "person_name")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentPerson {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "role_type", nullable = false, length = 20)
    private String roleType;

    @Column(name = "person_name", nullable = false, length = 100)
    private String personName;

    @Column(name = "character_name", length = 100)
    private String characterName;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "person_id", length = 100)
    private String personId;

    @Column(name = "person_img", columnDefinition = "TEXT")
    private String personImg;

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = Generators.timeBasedEpochGenerator().generate();
        }
    }
}
