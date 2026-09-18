package com.codeit.modoo_playlist.modulebatch.embedding.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

import com.codeit.modoo_playlist.infra.search.ContentEmbeddingDocument;

@ExtendWith(MockitoExtension.class)
class ContentEmbeddingIndexInitializerTest {

  @Mock private ElasticsearchOperations elasticsearchOperations;
  @Mock private IndexOperations indexOperations;

  @Test
  void 인덱스가_없으면_매핑과_함께_생성한다() {
    when(elasticsearchOperations.indexOps(ContentEmbeddingDocument.class)).thenReturn(indexOperations);
    when(indexOperations.exists()).thenReturn(false);

    new ContentEmbeddingIndexInitializer(elasticsearchOperations).run(null);

    verify(indexOperations).createWithMapping();
    verify(indexOperations, never()).getMapping();
  }

  @Test
  void 인덱스가_있고_벡터_필드_매핑도_있으면_통과한다() {
    when(elasticsearchOperations.indexOps(ContentEmbeddingDocument.class)).thenReturn(indexOperations);
    when(indexOperations.exists()).thenReturn(true);
    when(indexOperations.getMapping()).thenReturn(Map.of(
        "properties", Map.of(ContentEmbeddingDocument.VECTOR_FIELD, Map.of("type", "dense_vector"))
    ));

    assertThatCode(() -> new ContentEmbeddingIndexInitializer(elasticsearchOperations).run(null))
        .doesNotThrowAnyException();
    verify(indexOperations, never()).createWithMapping();
  }

  @Test
  void 인덱스는_있는데_벡터_필드_매핑이_없으면_예외를_던진다() {
    when(elasticsearchOperations.indexOps(ContentEmbeddingDocument.class)).thenReturn(indexOperations);
    when(indexOperations.exists()).thenReturn(true);
    when(indexOperations.getMapping()).thenReturn(Map.of(
        "properties", Map.of("title", Map.of("type", "keyword"))
    ));

    assertThatThrownBy(() -> new ContentEmbeddingIndexInitializer(elasticsearchOperations).run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(ContentEmbeddingDocument.INDEX_NAME)
        .hasMessageContaining(ContentEmbeddingDocument.VECTOR_FIELD);
  }
}
