package com.codeit.modoo_playlist.moduleapi.domain.message.repository;

import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID>,MessageRepositoryCustom {
}
