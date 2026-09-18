package com.codeit.modoo_playlist.modulebatch.embedding.config;

import com.codeit.modoo_playlist.infra.search.ContentEmbeddingDocument;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "batch.embedding", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ContentEmbeddingIndexInitializer implements ApplicationRunner {

  private final ElasticsearchOperations elasticsearchOperations;

  @Override
  public void run(ApplicationArguments args) {
    IndexOperations indexOps = elasticsearchOperations.indexOps(ContentEmbeddingDocument.class);

    if (!indexOps.exists()) {
      indexOps.createWithMapping();
      log.info("ES 인덱스 생성: {}", ContentEmbeddingDocument.INDEX_NAME);
      return;
    }

    verifyMapping(indexOps);
  }

  @SuppressWarnings("unchecked")
  private void verifyMapping(IndexOperations indexOps) {
    Map<String, Object> mapping = indexOps.getMapping();
    Object properties = mapping.get("properties");
    boolean hasVectorField = properties instanceof Map<?, ?> props
        && props.containsKey(ContentEmbeddingDocument.VECTOR_FIELD);

    if (!hasVectorField) {
      throw new IllegalStateException(
          ("%s 인덱스에 %s 매핑이 없습니다. 매핑 생성이 이전에 실패한 채로 남은 것으로 보입니다. "
              + "인덱스를 삭제 후 재기동하거나 수동으로 매핑을 추가하세요.")
              .formatted(ContentEmbeddingDocument.INDEX_NAME, ContentEmbeddingDocument.VECTOR_FIELD));
    }
  }
}
