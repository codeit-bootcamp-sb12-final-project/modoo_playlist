package com.codeit.modoo_playlist.core.domain.conversation.entity;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@SuperBuilder
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseUpdatableEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "participant_ids", columnDefinition = "json", nullable = false)
    @Builder.Default
    private List<UUID> participantIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ConversationType type;
}