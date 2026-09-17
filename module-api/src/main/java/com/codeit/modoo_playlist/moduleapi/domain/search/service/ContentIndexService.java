package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentIndexReader;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentIndexService {
  private static final String INDEX_NAME = "contents";

  private final ContentIndexReader contentIndexReader;
  private final ContentSearchRepository contentSearchRepository;
  private final ElasticsearchClient elasticsearchClient;

  private final Object indexLock = new Object();

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void index(UUID contentId) {
    Objects.requireNonNull(contentId, "콘텐츠 ID는 필수입니다.");

    synchronized (indexLock) {
      Optional<ContentDocument> optionalDocument = contentIndexReader.readOne(contentId);

      if (optionalDocument.isEmpty()) {
        contentSearchRepository.deleteById(contentId.toString());
        return;
      }

      contentSearchRepository.save(optionalDocument.get());
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public List<ContentDocument> indexBatch(UUID lastId, int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("조회 개수는 1 이상이어야 합니다.");
    }

    synchronized (indexLock) {
      List<ContentDocument> documents = contentIndexReader.read(lastId, size);

      if (documents.isEmpty()) {
        return List.of();
      }
      contentSearchRepository.saveAll(documents);

      return documents;
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void updateWatcherCount(UUID contentId) {
    Objects.requireNonNull(contentId, "콘텐츠 ID는 필수입니다.");

    synchronized (indexLock) {
      long watcherCount = contentIndexReader.readWatcherCount(contentId);

      try {
        elasticsearchClient.update(
            update -> update.index(INDEX_NAME)
                .id(contentId.toString())
                .doc(Map.of("watcherCount", watcherCount)),
            Object.class
        );
      } catch (ElasticsearchException exception) {
        if (exception.status() == 404
            && "document_missing_exception".equals(exception.error().type())) {
          index(contentId);
          return;
        }

        throw exception;
      } catch (IOException exception) {
        throw new IllegalStateException("ES 시청자 수 갱신 요청에 실패했습니다. contentId=" + contentId, exception);
      }
    }
  }
}
