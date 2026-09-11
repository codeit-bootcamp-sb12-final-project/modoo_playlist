package com.codeit.modoo_playlist.moduleapi.domain.conversation.repository;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID>, ConversationRepositoryCustom {
}
