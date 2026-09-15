package com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.service.impl;

import com.codeit.modoo_playlist.infra.search.ContentEmbeddingDocument;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.dto.SearchContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.service.SearchContentsService;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.util.SearchQueryTextBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchContentsServiceImpl implements SearchContentsService {

  private static final int CANDIDATE_MULTIPLIER = 10;

  private final ElasticsearchOperations elasticsearchOperations;
  private final EmbeddingModel embeddingModel;

  @Override
  public List<SearchContentDto> search(String query, Integer limit) {
    List<Float> queryVector = embedQuery(query);

    NativeQuery searchQuery = NativeQuery.builder()
        .withQuery(q -> q.knn(k -> k
            .field(ContentEmbeddingDocument.VECTOR_FIELD)
            .queryVector(queryVector)
            .k(limit)
            .numCandidates(limit * CANDIDATE_MULTIPLIER)))
        .withMaxResults(limit)
        .build();

    return elasticsearchOperations
        .search(searchQuery, ContentEmbeddingDocument.class)
        .stream()
        .map(SearchContentsServiceImpl::toDto)
        .toList();
  }

  private List<Float> embedQuery(String query) {
    EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(
        List.of(SearchQueryTextBuilder.build(query)), EmbeddingOptions.builder().build()));

    float[] output = response.getResult().getOutput();
    List<Float> vector = new ArrayList<>(output.length);
    for (float v : output) {
      vector.add(v);
    }
    return vector;
  }

  private static SearchContentDto toDto(SearchHit<ContentEmbeddingDocument> hit) {
    ContentEmbeddingDocument document = hit.getContent();
    return new SearchContentDto(
        UUID.fromString(document.contentId()),
        document.title(),
        document.thumbnailUrl(),
        hit.getScore()
    );
  }
}
