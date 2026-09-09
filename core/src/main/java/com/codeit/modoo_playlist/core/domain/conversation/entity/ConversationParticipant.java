package com.codeit.modoo_playlist.core.domain.conversation.entity;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.common.entity.baseentity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "conversation_participants",
        indexes ={@Index(name = "IDX_CONV_PARTICIPANTS_USER", columnList = "user_id")},
    uniqueConstraints ={
        @UniqueConstraint(
            name = "UK_CONVERSATION_PARTICIPANTS_CONVERSATION_USER",
            columnNames = {"conversation_id", "user_id"})
        })
@Getter
@SuperBuilder
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConversationParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private ConversationParticipant(Conversation conversation, User user) {
        this.conversation = conversation;
        this.user = user;
        conversation.getParticipants().add(this);
    }

    public static ConversationParticipant create(Conversation conversation, User user) {
        return new ConversationParticipant(conversation,user);
    }

}
