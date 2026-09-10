package com.codeit.modoo_playlist.moduleapi.domain.conversationParticipant;

import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationParticipant> findAllByConversation_Id(UUID conversationId);

    List<ConversationParticipant> findAllByUser_Id(UUID userId);

    Optional<ConversationParticipant> findByConversation_IdAndUser_Id(UUID conversationId, UUID userId);
}
