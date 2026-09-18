package com.codeit.modoo_playlist.modulebatch.embedding.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import com.codeit.modoo_playlist.infra.search.ContentEmbeddingDocument;
import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingResult;
import com.codeit.modoo_playlist.modulebatch.embedding.persistence.ContentEmbeddingMapper;

@ExtendWith(MockitoExtension.class)
class ContentEmbeddingWriterTest {

  @Mock private ContentEmbeddingMapper mapper;
  @Mock private ElasticsearchOperations elasticsearchOperations;
  @Mock private EmbeddingModel embeddingModel;

  @Test
  void 빈_청크는_아무것도_하지_않는다() throws Exception {
    new ContentEmbeddingWriter(mapper, elasticsearchOperations, embeddingModel).write(new Chunk<>(List.of()));

    verifyNoInteractions(mapper, elasticsearchOperations, embeddingModel);
  }

  @Test
  void 임베딩을_문서로_변환해_색인하고_해시를_갱신한다() throws Exception {
    ContentEmbeddingResult first = new ContentEmbeddingResult("c1", "text1", "제목1", "thumb1", "hash1");
    ContentEmbeddingResult second = new ContentEmbeddingResult("c2", "text2", "제목2", "thumb2", "hash2");
    when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(
        new EmbeddingResponse(List.of(
            new Embedding(new float[]{0.1f}, 0),
            new Embedding(new float[]{0.2f}, 1)
        )));

    new ContentEmbeddingWriter(mapper, elasticsearchOperations, embeddingModel)
        .write(new Chunk<>(List.of(first, second)));

    ArgumentCaptor<List<ContentEmbeddingDocument>> captor = ArgumentCaptor.forClass(List.class);
    verify(elasticsearchOperations).save(captor.capture());
    assertThatDocumentsMatch(captor.getValue(), first, second);

    verify(mapper).updateEmbeddingSourceHash("c1", "hash1");
    verify(mapper).updateEmbeddingSourceHash("c2", "hash2");
  }

  @Test
  void 임베딩_응답_개수가_요청과_다르면_예외를_던지고_저장하지_않는다() {
    ContentEmbeddingResult only = new ContentEmbeddingResult("c1", "text1", "제목1", "thumb1", "hash1");
    when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(
        new EmbeddingResponse(List.of()));

    assertThatThrownBy(() ->
        new ContentEmbeddingWriter(mapper, elasticsearchOperations, embeddingModel)
            .write(new Chunk<>(List.of(only)))
    ).isInstanceOf(IllegalStateException.class);

    verify(elasticsearchOperations, never()).save(anyList());
    verifyNoInteractions(mapper);
  }

  private void assertThatDocumentsMatch(
      List<ContentEmbeddingDocument> documents, ContentEmbeddingResult first, ContentEmbeddingResult second) {
    assertThat(documents).extracting(ContentEmbeddingDocument::contentId)
        .containsExactly(first.contentId(), second.contentId());
    assertThat(documents).extracting(ContentEmbeddingDocument::title)
        .containsExactly(first.title(), second.title());
  }
}
