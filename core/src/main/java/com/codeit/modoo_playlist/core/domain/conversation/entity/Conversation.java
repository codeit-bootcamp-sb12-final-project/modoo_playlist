package com.codeit.modoo_playlist.core.domain.conversation.entity;

import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations")
@Getter
@SuperBuilder
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseUpdatableEntity {

    @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<ConversationParticipant> participants = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ConversationType type;
}