package com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentQueryRepository;

public interface ContentRepository extends JpaRepository<Content, UUID>, ContentQueryRepository {

    Optional<Content> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByIdAndDeletedAtIsNull(UUID id);
}
