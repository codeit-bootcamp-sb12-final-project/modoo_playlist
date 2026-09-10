package com.codeit.modoo_playlist.core.domain.playlist.entity;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseUpdatableEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table (name = "playlists")

public class Playlist extends BaseUpdatableEntity {

    @Column(name = "owner_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 20)
    private GeneratedBy generatedBy;

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

}
