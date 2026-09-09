package com.codeit.modoo_playlist.core.domain.follow.entity;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Entity
@Table (
        name = "follows",
        uniqueConstraints = @UniqueConstraint(name = "UK_FOLLOWS_PAIR", columnNames = {"follower_id", "followee_id"})
)

public class Follow extends BaseEntity {

    @Column(name = "follower_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID followerId;

    @Column(name = "followee_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID followeeId;

}
