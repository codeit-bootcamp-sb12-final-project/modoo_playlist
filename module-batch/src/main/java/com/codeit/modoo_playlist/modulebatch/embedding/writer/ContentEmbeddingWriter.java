package com.codeit.modoo_playlist.modulebatch.embedding.writer;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingResult;
import com.codeit.modoo_playlist.modulebatch.embedding.persistence.ContentEmbeddingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
@RequiredArgsConstructor
public class ContentEmbeddingWriter implements ItemWriter<ContentEmbeddingResult> {

  private final ContentEmbeddingMapper mapper;

  @Override
  public void write(Chunk<? extends ContentEmbeddingResult> chunk) {
    for (ContentEmbeddingResult result : chunk) {
      mapper.upsertContentEmbedding(result);
      mapper.updateEmbeddingSourceHash(result.contentId(), result.sourceHash());
    }
  }
}
