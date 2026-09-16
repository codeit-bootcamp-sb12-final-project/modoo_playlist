package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import java.util.List;
import java.util.UUID;

public interface RecommendationService {

  List<RecommendedContentDto> getSimilarContents(UUID contentId, Integer limit);

  List<RecommendedContentDto> getRecommendationsForMe(UUID userId, Integer limit);

  List<RecommendedContentDto> getTrendingContents(Integer limit);

  List<RecommendedContentDto> getTopTagMatchContents(UUID tagId, Integer limit);
}
