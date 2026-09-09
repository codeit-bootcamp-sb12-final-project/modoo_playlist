package com.codeit.modoo_playlist.core.domain.playlist.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable

public class PlaylistContentId implements Serializable {

    @Column(name = "playlist_id", columnDefinition = "BINARY(16)")
    private UUID playlistId;

    @Column(name = "content_id", columnDefinition = "BINARY(16)")
    private UUID contentId;

}
