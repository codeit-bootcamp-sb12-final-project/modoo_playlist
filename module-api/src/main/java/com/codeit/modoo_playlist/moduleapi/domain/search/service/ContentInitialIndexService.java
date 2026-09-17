package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentInitialIndexService {

  private static final int BATCH_SIZE = 100;

  private final ContentIndexService contentIndexService;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public long indexAll() {
    UUID lastId = null;
    long indexedCount = 0;

    while (true) {
      List<ContentDocument> documents =
          contentIndexService.indexBatch(lastId, BATCH_SIZE);

      if (documents.isEmpty()) {
        return indexedCount;
      }

      indexedCount += documents.size();
      lastId = UUID.fromString(
          documents.get(documents.size() - 1).getId()
      );
    }
  }

}
