package com.codeit.modoo_playlist.core.domain.embedding.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeit.modoo_playlist.core.domain.embedding.entity.ContentEmbedding;

public interface ContentEmbeddingRepository extends JpaRepository<ContentEmbedding, UUID> {
}
