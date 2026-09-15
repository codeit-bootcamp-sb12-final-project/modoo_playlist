package com.codeit.modoo_playlist.modulebatch.embedding.config;

import com.codeit.modoo_playlist.infra.search.ContentEmbeddingDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentEmbeddingIndexInitializer implements ApplicationRunner {

  private final ElasticsearchOperations elasticsearchOperations;

  @Override
  public void run(ApplicationArguments args) {
    var indexOps = elasticsearchOperations.indexOps(ContentEmbeddingDocument.class);
    if (indexOps.exists()) {
      return;
    }
    indexOps.create();
    indexOps.putMapping();
    log.info("ES 인덱스 생성: {}", ContentEmbeddingDocument.INDEX_NAME);
  }
}
