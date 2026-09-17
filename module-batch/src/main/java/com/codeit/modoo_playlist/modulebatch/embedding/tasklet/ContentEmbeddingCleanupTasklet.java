package com.codeit.modoo_playlist.modulebatch.embedding.tasklet;

import com.codeit.modoo_playlist.infra.search.ContentEmbeddingDocument;
import com.codeit.modoo_playlist.modulebatch.embedding.persistence.ContentEmbeddingMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;

@Slf4j
@RequiredArgsConstructor
public class ContentEmbeddingCleanupTasklet implements Tasklet {

  private static final int PAGE_SIZE = 200;

  private final ContentEmbeddingMapper mapper;
  private final ElasticsearchOperations elasticsearchOperations;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    int total = 0;
    List<String> page;
    while (!(page = mapper.findDeletedContentIdsNeedingCleanup(PAGE_SIZE)).isEmpty()) {
      deleteFromEs(page);
      mapper.clearEmbeddingSourceHashBulk(page);
      total += page.size();
    }
    if (total > 0) {
      log.info("삭제된 콘텐츠의 임베딩 문서 정리: {}건", total);
    }
    return RepeatStatus.FINISHED;
  }

  private void deleteFromEs(List<String> contentIds) {
    NativeQuery idsQuery = NativeQuery.builder()
        .withQuery(q -> q.ids(i -> i.values(contentIds)))
        .build();
    elasticsearchOperations.delete(
        DeleteQuery.builder(idsQuery).build(), ContentEmbeddingDocument.class);
  }
}
