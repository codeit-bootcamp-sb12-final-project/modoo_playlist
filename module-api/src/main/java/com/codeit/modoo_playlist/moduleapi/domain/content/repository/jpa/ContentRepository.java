package com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentQueryRepository;

public interface ContentRepository extends JpaRepository<Content, UUID>, ContentQueryRepository {

    Optional<Content> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByIdAndDeletedAtIsNull(UUID id);

    @Query("select c.id from Content c where c.id in :ids and c.deletedAt is null")
    List<UUID> findAliveIds(@Param("ids") Collection<UUID> ids);
}
