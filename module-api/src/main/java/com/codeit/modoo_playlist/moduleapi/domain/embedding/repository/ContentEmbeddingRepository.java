package com.codeit.modoo_playlist.moduleapi.domain.embedding.repository;

import com.codeit.modoo_playlist.core.domain.embedding.entity.ContentEmbedding;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentEmbeddingRepository extends JpaRepository<ContentEmbedding, UUID> {
}
