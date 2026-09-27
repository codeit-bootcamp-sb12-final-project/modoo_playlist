package com.codeit.modoo_playlist.moduleapi.domain.message.repository;

import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID>, MessageRepositoryCustom {

    @Modifying(flushAutomatically = true)
    @Query("delete from Message m where m.sender.id = :userId or m.receiver.id = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
