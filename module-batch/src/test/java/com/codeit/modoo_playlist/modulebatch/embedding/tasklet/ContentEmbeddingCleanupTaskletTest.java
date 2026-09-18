package com.codeit.modoo_playlist.modulebatch.embedding.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;

import com.codeit.modoo_playlist.infra.search.ContentEmbeddingDocument;
import com.codeit.modoo_playlist.modulebatch.embedding.persistence.ContentEmbeddingMapper;

@ExtendWith(MockitoExtension.class)
class ContentEmbeddingCleanupTaskletTest {

  @Mock private ContentEmbeddingMapper mapper;
  @Mock private ElasticsearchOperations elasticsearchOperations;

  @Test
  void 정리할_대상이_없으면_ES와_DB_모두_건드리지_않는다() throws Exception {
    when(mapper.findDeletedContentIdsNeedingCleanup(200)).thenReturn(List.of());

    RepeatStatus status = new ContentEmbeddingCleanupTasklet(mapper, elasticsearchOperations)
        .execute(null, null);

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verifyNoInteractions(elasticsearchOperations);
    verify(mapper, never()).clearEmbeddingSourceHashBulk(any());
  }

  @Test
  void 정리_대상을_ES에서_지우고_해시를_초기화하며_빈_페이지가_나올때까지_반복한다() throws Exception {
    List<String> page = List.of("c1", "c2");
    when(mapper.findDeletedContentIdsNeedingCleanup(200))
        .thenReturn(page)
        .thenReturn(List.of());

    RepeatStatus status = new ContentEmbeddingCleanupTasklet(mapper, elasticsearchOperations)
        .execute(null, null);

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(elasticsearchOperations, times(1))
        .delete(any(DeleteQuery.class), eq(ContentEmbeddingDocument.class));
    verify(mapper).clearEmbeddingSourceHashBulk(page);
    verify(mapper, times(2)).findDeletedContentIdsNeedingCleanup(200);
  }
}
