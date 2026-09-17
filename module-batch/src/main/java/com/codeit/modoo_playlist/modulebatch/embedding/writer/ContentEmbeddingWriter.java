package com.codeit.modoo_playlist.modulebatch.embedding.writer;

import com.codeit.modoo_playlist.infra.search.ContentEmbeddingDocument;
import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingResult;
import com.codeit.modoo_playlist.modulebatch.embedding.persistence.ContentEmbeddingMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@Slf4j
@RequiredArgsConstructor
public class ContentEmbeddingWriter implements ItemWriter<ContentEmbeddingResult> {

  private final ContentEmbeddingMapper mapper;
  private final ElasticsearchOperations elasticsearchOperations;
  private final EmbeddingModel embeddingModel;

  @Override
  public void write(Chunk<? extends ContentEmbeddingResult> chunk) {
    List<? extends ContentEmbeddingResult> items = chunk.getItems();
    if (items.isEmpty()) {
      return;
    }

    List<String> texts = items.stream().map(ContentEmbeddingResult::text).toList();
    EmbeddingResponse response = embeddingModel.call(
        new EmbeddingRequest(texts, EmbeddingOptions.builder().build()));

    if (response.getResults().size() != items.size()) {
      throw new IllegalStateException(
          "임베딩 응답 개수 불일치: 요청 %d건, 응답 %d건"
              .formatted(items.size(), response.getResults().size()));
    }

    List<ContentEmbeddingDocument> documents = new ArrayList<>(items.size());
    for (int i = 0; i < items.size(); i++) {
      ContentEmbeddingResult item = items.get(i);
      documents.add(new ContentEmbeddingDocument(
          item.contentId(),
          response.getResults().get(i).getOutput(),
          item.title(),
          item.thumbnailUrl()));
    }
    elasticsearchOperations.save(documents);

    for (ContentEmbeddingResult item : items) {
      mapper.updateEmbeddingSourceHash(item.contentId(), item.sourceHash());
    }
    log.info("콘텐츠 임베딩 색인: {}건", documents.size());
  }
}
