package com.codeit.modoo_playlist.moduleapi.domain.embedding.repository;

import com.codeit.modoo_playlist.core.domain.embedding.entity.ContentEmbedding;
import com.codeit.modoo_playlist.moduleapi.dto.embedding.EmbeddingCandidate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContentEmbeddingRepository extends JpaRepository<ContentEmbedding, UUID> {

  @Query("""
      select new com.codeit.modoo_playlist.moduleapi.dto.embedding.EmbeddingCandidate(
        ce.contents.id, ce.contents.title, ce.contents.thumbnailUrl, ce.vector
      )
      from ContentEmbedding ce
      where ce.contents.deletedAt is null
      """)
  List<EmbeddingCandidate> findAllCandidates();
}
