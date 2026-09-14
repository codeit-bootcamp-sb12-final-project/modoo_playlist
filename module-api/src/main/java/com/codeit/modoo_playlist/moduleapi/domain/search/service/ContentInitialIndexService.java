package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentIndexReader;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentInitialIndexService {

  private static final int BATCH_SIZE = 100;

  private final ContentIndexReader contentIndexReader;
  private final ContentSearchRepository contentSearchRepository;

  public long indexAll() {
    int start = 0;
    long indexedCount = 0;

    while (true) {
      List<ContentDocument> documents =
          contentIndexReader.read(start, BATCH_SIZE);

      if (documents.isEmpty()) {
        return indexedCount;
      }

      contentSearchRepository.saveAll(documents);

      indexedCount += documents.size();
      start += documents.size();
    }
  }

}
