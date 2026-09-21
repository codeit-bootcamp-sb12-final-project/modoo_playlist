package com.codeit.modoo_playlist.infra.repository;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface RealtimeContentRepository extends Repository<Content, UUID> {
    Optional<Content> findById(UUID contentId);
}
