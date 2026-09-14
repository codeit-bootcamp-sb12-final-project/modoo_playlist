package com.codeit.modoo_playlist.modulebatch.embedding.reader;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;
import com.codeit.modoo_playlist.modulebatch.embedding.persistence.ContentEmbeddingMapper;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemReader;

@RequiredArgsConstructor
public class ContentEmbeddingReader implements ItemReader<ContentEmbeddingTarget> {

  private static final int PAGE_SIZE = 100;

  private final ContentEmbeddingMapper mapper;

  private int offset = 0;
  private Iterator<ContentEmbeddingTarget> currentPage = List.<ContentEmbeddingTarget>of()
      .iterator();

  @Override
  public @Nullable ContentEmbeddingTarget read() {
    if (!currentPage.hasNext()) {
      List<ContentEmbeddingTarget> page = mapper.findContentsNeedingEmbedding(offset, PAGE_SIZE);
      if (page.isEmpty()) {
        return null;
      }
      offset += PAGE_SIZE;
      currentPage = page.iterator();
    }
    return currentPage.next();
  }
}
