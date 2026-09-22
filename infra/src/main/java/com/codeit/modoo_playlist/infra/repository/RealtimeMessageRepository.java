package com.codeit.modoo_playlist.infra.repository;

import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface RealtimeMessageRepository extends Repository<Message, UUID> {
    Message save(Message message);
}
