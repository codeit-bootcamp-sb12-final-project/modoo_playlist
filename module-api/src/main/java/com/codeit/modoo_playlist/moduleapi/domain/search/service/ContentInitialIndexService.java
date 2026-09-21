package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ReindexStateRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentInitialIndexService implements ApplicationRunner {

  private static final int BATCH_SIZE = 100;
  private static final int INDEX_VERSION = 2;
  private static final String INDEX_ALIAS = "contents";
  private static final String INDEX_PREFIX = "contents_v";

  private final ContentIndexService contentIndexService;
  private final ReindexStateRepository reindexStateRepository;

  private String getTargetIndexName() {
    return INDEX_PREFIX + INDEX_VERSION;
  }

  private boolean isReindexRequired() {
    String targetIndexName = getTargetIndexName();

    return contentIndexService.getAliasIndex(INDEX_ALIAS)
        .map(currentIndexName -> !currentIndexName.equals(targetIndexName))
        .orElse(true);
  }

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void run(ApplicationArguments args) {
    if (!isReindexRequired()) {
      log.info("콘텐츠 검색 인덱스가 최신 버전입니다. index={}", getTargetIndexName());
      return;
    }

    String targetIndexName = getTargetIndexName();

    if (!reindexStateRepository.start(targetIndexName)) {
      log.info("다른 인스턴스에서 콘텐츠 재색인을 진행 중입니다. index={}", targetIndexName);
      return;
    }

    try {
      if (!isReindexRequired()) {
        log.info("이미 콘텐츠 재색인이 완료되었습니다. index={}", targetIndexName);
        return;
      }

      if (contentIndexService.concreteIndexExists(targetIndexName)) {
        contentIndexService.deleteIndex(targetIndexName);
      }

      contentIndexService.createIndex(targetIndexName);
      log.info("콘텐츠 전체 재색인을 시작합니다. index={}", targetIndexName);

      long count = indexAll(targetIndexName);
      applyChangedContents(targetIndexName);

      if (!reindexStateRepository.startSwitching()) {
        throw new IllegalStateException("콘텐츠 검색 인덱스 전환 상태 변경에 실패했습니다. index=" + targetIndexName);
      }

      contentIndexService.finalizeReindex(
          INDEX_ALIAS, targetIndexName, () -> applyChangedContents(targetIndexName));

      log.info("콘텐츠 전체 재색인 및 인덱스 전환 완료: {}건, index={}", count, targetIndexName);
    } catch (Exception exception) {
      if (!targetIndexName.equals(
          contentIndexService.getAliasIndex(INDEX_ALIAS).orElse(null))) {
        contentIndexService.deleteIndex(targetIndexName);
      }
      throw exception;
    } finally {
      reindexStateRepository.finish();
    }
  }

  private void applyChangedContents(String indexName) {
    while (true) {
      String contentId = reindexStateRepository.popChangedContentId();

      if (contentId == null) {
        return;
      }
      contentIndexService.index(UUID.fromString(contentId), indexName);
    }
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public long indexAll(String indexName) {
    UUID lastId = null;
    long indexedCount = 0;

    while (true) {
      List<ContentDocument> documents = contentIndexService.indexBatch(lastId, BATCH_SIZE, indexName);

      if (documents.isEmpty()) {
        return indexedCount;
      }

      indexedCount += documents.size();
      lastId = UUID.fromString(documents.get(documents.size() - 1).getId());
    }
  }

}
