package com.codeit.modoo_playlist.modulebatch.embedding.tasklet;

import com.codeit.modoo_playlist.infra.search.ContentEmbeddingDocument;
import com.codeit.modoo_playlist.modulebatch.embedding.persistence.ContentEmbeddingMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@Slf4j
@RequiredArgsConstructor
public class ContentEmbeddingCleanupTasklet implements Tasklet {

  private final ContentEmbeddingMapper mapper;
  private final ElasticsearchOperations elasticsearchOperations;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    List<String> contentIds = mapper.findDeletedContentIdsNeedingCleanup();
    for (String contentId : contentIds) {
      elasticsearchOperations.delete(contentId, ContentEmbeddingDocument.class);
      mapper.clearEmbeddingSourceHash(contentId);
    }
    if (!contentIds.isEmpty()) {
      log.info("삭제된 콘텐츠의 임베딩 문서 정리: {}건", contentIds.size());
    }
    return RepeatStatus.FINISHED;
  }
}
