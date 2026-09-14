package com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.service.impl;

import com.codeit.modoo_playlist.core.global.common.util.CosineSimilarity;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.service.SearchContentsService;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.util.SearchQueryTextBuilder;
import com.codeit.modoo_playlist.moduleapi.domain.embedding.repository.ContentEmbeddingRepository;
import com.codeit.modoo_playlist.moduleapi.dto.chat.tool.SearchContentDto;
import com.codeit.modoo_playlist.moduleapi.dto.embedding.EmbeddingCandidate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchContentsServiceImpl implements SearchContentsService {

  private final ContentEmbeddingRepository contentEmbeddingRepository;
  private final EmbeddingModel embeddingModel;

  @Override
  public List<SearchContentDto> search(String query, Integer limit) {
    List<EmbeddingCandidate> candidates = contentEmbeddingRepository.findAllCandidates();
    if (candidates.isEmpty()) {
      return List.of();
    }

    String queryText = SearchQueryTextBuilder.build(query);
    EmbeddingResponse response = embeddingModel.call(
        new EmbeddingRequest(List.of(queryText), EmbeddingOptions.builder().build()));

    List<Double> queryVector = new ArrayList<>();
    for (float v : response.getResult().getOutput()) {
      queryVector.add((double) v);
    }

    return candidates.stream()
        .map(c -> new SearchContentDto(
            c.contentId(), c.title(), c.thumbnailUrl(),
            CosineSimilarity.compute(queryVector, c.vector())
        ))
        .sorted(Comparator.comparingDouble(SearchContentDto::score).reversed())
        .limit(limit)
        .toList();
  }

}
