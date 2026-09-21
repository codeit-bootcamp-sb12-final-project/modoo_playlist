package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import com.codeit.modoo_playlist.infra.search.ContentEmbeddingDocument;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceImplTest {

  @Mock private ElasticsearchOperations elasticsearchOperations;
  @Mock private EmbeddingModel embeddingModel;
  @Mock private ContentRepository contentRepository;
  @Mock private SearchHits<ContentEmbeddingDocument> searchHits;

  private SemanticSearchServiceImpl service() {
    return new SemanticSearchServiceImpl(elasticsearchOperations, embeddingModel, contentRepository);
  }

  @Test
  void 검색결과가_없으면_빈_리스트를_반환한다() {
    stubEmbedding();
    when(searchHits.getSearchHits()).thenReturn(List.of());
    when(elasticsearchOperations.search(any(Query.class), eq(ContentEmbeddingDocument.class)))
        .thenReturn(searchHits);
    when(contentRepository.findAliveIds(anyCollection())).thenReturn(List.of());

    assertThat(service().search("우울할 때 볼 영화", 5)).isEmpty();
  }

  @Test
  void 삭제되지_않고_살아있는_콘텐츠만_결과에_포함한다() {
    stubEmbedding();
    UUID aliveId = UUID.randomUUID();
    UUID deletedId = UUID.randomUUID();
    SearchHit<ContentEmbeddingDocument> aliveHit = hit(aliveId, "살아있는 콘텐츠", 0.9f);
    SearchHit<ContentEmbeddingDocument> deletedHit = hit(deletedId, "삭제된 콘텐츠", 0.8f);
    when(searchHits.getSearchHits()).thenReturn(List.of(aliveHit, deletedHit));
    when(elasticsearchOperations.search(any(Query.class), eq(ContentEmbeddingDocument.class)))
        .thenReturn(searchHits);
    when(contentRepository.findAliveIds(anyCollection())).thenReturn(List.of(aliveId));

    List<RecommendedContentDto> result = service().search("느와르 영화", 5);

    assertThat(result).extracting(RecommendedContentDto::contentId).containsExactly(aliveId);
    assertThat(result.get(0).title()).isEqualTo("살아있는 콘텐츠");
    assertThat(result.get(0).score()).isEqualTo(0.9f);
  }

  @Test
  void 쿼리를_비대칭_검색_포맷으로_임베딩해서_요청한다() {
    stubEmbedding();
    when(searchHits.getSearchHits()).thenReturn(List.of());
    when(elasticsearchOperations.search(any(Query.class), eq(ContentEmbeddingDocument.class)))
        .thenReturn(searchHits);
    when(contentRepository.findAliveIds(anyCollection())).thenReturn(List.of());

    service().search("우울할 때 볼 영화", 5);

    ArgumentCaptor<EmbeddingRequest> captor = ArgumentCaptor.forClass(EmbeddingRequest.class);
    verify(embeddingModel).call(captor.capture());
    assertThat(captor.getValue().getInstructions())
        .containsExactly("task: search result | query: 우울할 때 볼 영화");
  }

  private void stubEmbedding() {
    when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(
        new EmbeddingResponse(List.of(new Embedding(new float[]{0.1f, 0.2f}, 0))));
  }

  @SuppressWarnings("unchecked")
  private SearchHit<ContentEmbeddingDocument> hit(UUID contentId, String title, float score) {
    SearchHit<ContentEmbeddingDocument> hit = mock(SearchHit.class);
    when(hit.getContent()).thenReturn(
        new ContentEmbeddingDocument(contentId.toString(), new float[]{0.1f}, title, "thumb"));
    lenient().when(hit.getScore()).thenReturn(score);
    return hit;
  }
}
