package com.codeit.modoo_playlist.moduleapi.domain.message.repository;

import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID>,MessageRepositoryCustom {

    Slice<Message> findAllByConversation_IdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    Optional<Message> findByIdAndConversationIdAndReceiverId(
            UUID id,
            UUID conversationId,
            UUID receiverId
    );
}
