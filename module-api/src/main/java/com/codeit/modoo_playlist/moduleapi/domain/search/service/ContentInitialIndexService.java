package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentInitialIndexService implements ApplicationRunner {

  private static final int BATCH_SIZE = 100;

  private final ContentIndexService contentIndexService;
  private final ElasticsearchOperations elasticsearchOperations;

  @Value("${search.reindex-on-startup:false}")
  private boolean reindexOnStartup;

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void run(ApplicationArguments args) {
    if (!reindexOnStartup) {
      return;
    }

    IndexOperations indexOperations = elasticsearchOperations.indexOps(ContentDocument.class);

    if (!indexOperations.exists()) {
      if (!indexOperations.createWithMapping()) {
        throw new IllegalStateException("콘텐츠 검색 인덱스 생성에 실패했습니다.");
      }
    } else if (!indexOperations.putMapping()) {
      throw new IllegalStateException("콘텐츠 검색 매핑 적용에 실패했습니다.");
    }

    log.info("콘텐츠 전체 재색인을 시작합니다.");

    long count = indexAll();
    indexOperations.refresh();

    log.info("콘텐츠 전체 재색인 완료: {}건", count);
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public long indexAll() {
    UUID lastId = null;
    long indexedCount = 0;

    while (true) {
      List<ContentDocument> documents = contentIndexService.indexBatch(lastId, BATCH_SIZE);

      if (documents.isEmpty()) {
        return indexedCount;
      }

      indexedCount += documents.size();
      lastId = UUID.fromString(documents.get(documents.size() - 1).getId()
      );
    }
  }

}
