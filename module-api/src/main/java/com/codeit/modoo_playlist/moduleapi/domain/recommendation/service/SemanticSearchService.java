package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import java.util.List;

public interface SemanticSearchService {
  List<RecommendedContentDto> search(String query, Integer limit);
}
