package com.codeit.modoo_playlist.infra.repository;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface RealtimeConversationRepository extends Repository<Conversation, UUID> {

    boolean existsByIdAndParticipants_User_Id(UUID conversationId, UUID userId);

    Optional<Conversation> findById(UUID conversationId);
}
