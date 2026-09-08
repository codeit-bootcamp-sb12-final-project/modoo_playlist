package com.codeit.modoo_playlist.core.domain.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentTagId implements Serializable {

    @Column(name = "content_id", columnDefinition = "BINARY(16)")
    private UUID contentId;

    @Column(name = "tag_id", columnDefinition = "BINARY(16)")
    private UUID tagId;
}
