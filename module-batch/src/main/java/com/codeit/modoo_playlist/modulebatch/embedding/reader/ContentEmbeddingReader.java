package com.codeit.modoo_playlist.modulebatch.embedding.reader;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;
import com.codeit.modoo_playlist.modulebatch.embedding.persistence.ContentEmbeddingMapper;
import java.util.Iterator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemReader;

/**
 * maxItems는 폭주 방지용 상한. 걸리면 거기서 끊고 남은 건 다음 실행이 이어서 처리한다.
 */
@RequiredArgsConstructor
public class ContentEmbeddingReader implements ItemReader<ContentEmbeddingTarget> {

  private static final int PAGE_SIZE = 100;

  private final ContentEmbeddingMapper mapper;
  private final int maxItems;

  private int emitted = 0;
  private int offset = 0;
  private Iterator<ContentEmbeddingTarget> currentPage = List.<ContentEmbeddingTarget>of()
      .iterator();

  @Override
  public @Nullable ContentEmbeddingTarget read() {
    if (emitted >= maxItems) {
      return null;
    }
    if (!currentPage.hasNext()) {
      List<ContentEmbeddingTarget> page = mapper.findContentsNeedingEmbedding(offset, PAGE_SIZE);
      if (page.isEmpty()) {
        return null;
      }
      offset += PAGE_SIZE;
      currentPage = page.iterator();
    }
    emitted++;
    return currentPage.next();
  }
}
