package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.SimilarContentDto;
import java.util.List;
import java.util.UUID;

public interface RecommendationService {

  List<SimilarContentDto> getSimilarContents(UUID contentId, Integer limit);
}
