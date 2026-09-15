package com.codeit.modoo_playlist.modulebatch.embedding.persistence;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContentEmbeddingMapper {
  List<ContentEmbeddingTarget> findContentsNeedingEmbedding(@Param("offset") int offset, @Param("limit") int limit);

  void updateEmbeddingSourceHash(@Param("contentId") String contentId, @Param("hash") String hash);
}
