package com.codeit.modoo_playlist.moduleapi.domain.conversation.repository;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository  extends JpaRepository<Conversation, UUID> {
}
