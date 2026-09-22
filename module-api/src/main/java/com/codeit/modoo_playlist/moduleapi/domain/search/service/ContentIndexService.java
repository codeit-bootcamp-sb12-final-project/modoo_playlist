package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentIndexReader;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ReindexStateRepository;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
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
  private final ReindexStateRepository reindexStateRepository;
  private final ElasticsearchOperations elasticsearchOperations;

  private final Object indexLock = new Object();

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void index(UUID contentId) {
    Objects.requireNonNull(contentId, "콘텐츠 ID는 필수입니다.");

    synchronized (indexLock) {
      reindexStateRepository.recordChangedContent(contentId);

      Optional<ContentDocument> optionalDocument = contentIndexReader.readOne(contentId);

      if (optionalDocument.isEmpty()) {
        contentSearchRepository.deleteById(contentId.toString());
      } else {
        contentSearchRepository.save(optionalDocument.get());
      }

      if (reindexStateRepository.isSwitching()) {
        String targetIndexName = reindexStateRepository.getTargetIndex();

        if (targetIndexName != null && !targetIndexName.isBlank()) {
          index(contentId, targetIndexName);
        }
      }
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void index(UUID contentId, String indexName) {
    Objects.requireNonNull(contentId, "콘텐츠 ID는 필수입니다.");

    if (indexName == null || indexName.isBlank()) {
      throw new IllegalArgumentException("인덱스 이름은 필수입니다.");
    }

    synchronized (indexLock) {
      Optional<ContentDocument> optionalDocument = contentIndexReader.readOne(contentId);

      try {
        if (optionalDocument.isEmpty()) {
          elasticsearchClient.delete(request -> request
              .index(indexName)
              .id(contentId.toString()));
          return;
        }

        ContentDocument document = optionalDocument.get();

        elasticsearchClient.index(request -> request
            .index(indexName)
            .id(document.getId())
            .document(document));
      } catch (ElasticsearchException exception) {
        if (exception.status() == 404 && optionalDocument.isEmpty()) {
          return;
        }
        throw exception;
      } catch (IOException exception) {
        throw new IllegalStateException(
            "ES 콘텐츠 색인 요청에 실패했습니다. index=" + indexName + ", contentId=" + contentId, exception);
      }
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
  public List<ContentDocument> indexBatch(UUID lastId, int size, String indexName) {
    if (size <= 0) {
      throw new IllegalArgumentException("조회 개수는 1 이상이어야 합니다.");
    }
    if (indexName == null || indexName.isBlank()) {
      throw new IllegalArgumentException("인덱스 이름은 필수입니다.");
    }

    synchronized (indexLock) {
      List<ContentDocument> documents = contentIndexReader.read(lastId, size);

      if (documents.isEmpty()) {
        return List.of();
      }

      try {
        for (ContentDocument document : documents) {
          elasticsearchClient.index(request -> request
              .index(indexName)
              .id(document.getId())
              .document(document));
        }
      } catch (IOException exception) {
        throw new IllegalStateException(
            "ES 콘텐츠 색인 요청에 실패했습니다. index=" + indexName, exception);
      }

      return documents;
    }
  }

  public boolean concreteIndexExists(String indexName) {
    if (indexName == null || indexName.isBlank()) {
      return false;
    }

    if (getAliasIndex(indexName).isPresent()) {
      return false;
    }

    try {
      return elasticsearchClient.indices()
          .exists(request -> request.index(indexName))
          .value();
    } catch (IOException exception) {
      throw new IllegalStateException("ES 인덱스 존재 여부 조회에 실패했습니다. index=" + indexName, exception);
    }
  }

  public Optional<String> getAliasIndex(String aliasName) {
    if (aliasName == null || aliasName.isBlank()) {
      return Optional.empty();
    }

    try {
      var response = elasticsearchClient.indices()
          .getAlias(request -> request.name(aliasName));

      return response.aliases().keySet().stream().findFirst();
    } catch (ElasticsearchException exception) {
      if (exception.status() == 404) {
        return Optional.empty();
      }
      throw exception;
    } catch (IOException exception) {
      throw new IllegalStateException("ES alias 조회에 실패했습니다. alias=" + aliasName, exception);
    }
  }

  public void finalizeReindex(String aliasName, String targetIndexName, Runnable applyChangedContents) {
    synchronized (indexLock) {
      applyChangedContents.run();
      refreshAndValidate(targetIndexName);
      switchAlias(aliasName, targetIndexName);
    }
  }

  public void refreshAndValidate(String indexName) {
    if (indexName == null || indexName.isBlank()) {
      throw new IllegalArgumentException("인덱스 이름은 필수입니다.");
    }

    try {
      var refreshResponse = elasticsearchClient.indices().refresh(request -> request.index(indexName));

      if (refreshResponse.shards().failed().intValue() > 0) {
        throw new IllegalStateException("ES refresh에 실패했습니다. index=" + indexName);
      }

      var missingFields = elasticsearchClient.count(request -> request.index(indexName)
          .query(query -> query.bool(bool -> bool
              .mustNot(not -> not.exists(exists -> exists.field("normalizedTitle"))))));

      if (missingFields.shards().failed().intValue() > 0) {
        throw new IllegalStateException("ES 문서 검증에 실패했습니다. index=" + indexName);
      }

      if (missingFields.count() > 0) {
        throw new IllegalStateException(
            "normalizedTitle 누락 문서가 있습니다. index=" + indexName + ", count=" + missingFields.count());
      }
    } catch (IOException exception) {
      throw new IllegalStateException("ES refresh 및 문서 검증 요청에 실패했습니다. index=" + indexName, exception);
    }
  }

  public void switchAlias(String aliasName, String indexName) {
    if (aliasName == null || aliasName.isBlank()) {
      throw new IllegalArgumentException("alias 이름은 필수입니다.");
    }
    if (indexName == null || indexName.isBlank()) {
      throw new IllegalArgumentException("인덱스 이름은 필수입니다.");
    }
    if (aliasName.equals(indexName)) {
      throw new IllegalArgumentException("alias와 실제 인덱스 이름은 달라야 합니다.");
    }

    try {
      Optional<String> currentIndex = getAliasIndex(aliasName);
      boolean legacyIndexExists = currentIndex.isEmpty() && concreteIndexExists(aliasName);

      var response = elasticsearchClient.indices().updateAliases(request -> {
        if (currentIndex.isPresent()) {
          request.actions(action -> action.remove(remove -> remove
              .index(currentIndex.get())
              .alias(aliasName)
              .mustExist(true)));
        } else if (legacyIndexExists) {
          request.actions(action -> action.removeIndex(remove -> remove.index(aliasName)));
        }

        return request.actions(action -> action.add(add -> add
            .index(indexName)
            .alias(aliasName)
            .isWriteIndex(true)));
      });

      if (!response.acknowledged()) {
        throw new IllegalStateException(
            "ES alias 전환 확인 응답을 받지 못했습니다. alias=" + aliasName);
      }

      if (!getAliasIndex(aliasName).filter(indexName::equals).isPresent()) {
        throw new IllegalStateException(
            "ES alias가 대상 인덱스를 가리키지 않습니다. alias=" + aliasName);
      }
    } catch (IOException exception) {
      throw new IllegalStateException(
          "ES alias 전환에 실패했습니다. alias=" + aliasName + ", index=" + indexName, exception);
    }
  }

  public void deleteIndex(String indexName) {
    if (indexName == null || indexName.isBlank()) {
      throw new IllegalArgumentException("인덱스 이름은 필수입니다.");
    }

    try {
      if (!concreteIndexExists(indexName)) {
        return;
      }
      elasticsearchClient.indices().delete(request -> request.index(indexName));
    } catch (IOException exception) {
      throw new IllegalStateException("ES 인덱스 삭제에 실패했습니다. index=" + indexName, exception);
    }
  }

  public void createIndex(String indexName) {
    if (indexName == null || indexName.isBlank()) {
      throw new IllegalArgumentException("인덱스 이름은 필수입니다.");
    }

    IndexOperations indexOperations = elasticsearchOperations.indexOps(IndexCoordinates.of(indexName));

    if (indexOperations.exists()) {
      return;
    }

    if (!indexOperations.create(
        indexOperations.createSettings(ContentDocument.class),
        indexOperations.createMapping(ContentDocument.class))) {
      throw new IllegalStateException("콘텐츠 검색 인덱스 생성에 실패했습니다. index=" + indexName);
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void updateWatcherCount(UUID contentId) {
    Objects.requireNonNull(contentId, "콘텐츠 ID는 필수입니다.");

    synchronized (indexLock) {
      reindexStateRepository.recordChangedContent(contentId);
      long watcherCount = contentIndexReader.readWatcherCount(contentId);

      try {
        elasticsearchClient.update(
            update -> update.index(INDEX_NAME)
                .id(contentId.toString())
                .doc(Map.of("watcherCount", watcherCount)),
            Object.class
        );
      } catch (ElasticsearchException exception) {
        if (exception.status() == 404 && "document_missing_exception".equals(exception.error().type())) {
          index(contentId);
          return;
        }
        throw exception;
      } catch (IOException exception) {
        throw new IllegalStateException("ES 시청자 수 갱신 요청에 실패했습니다. contentId=" + contentId, exception);
      }

      if (reindexStateRepository.isSwitching()) {
        String targetIndexName = reindexStateRepository.getTargetIndex();

        if (targetIndexName != null && !targetIndexName.isBlank()) {
          index(contentId, targetIndexName);
        }
      }
    }
  }
}
