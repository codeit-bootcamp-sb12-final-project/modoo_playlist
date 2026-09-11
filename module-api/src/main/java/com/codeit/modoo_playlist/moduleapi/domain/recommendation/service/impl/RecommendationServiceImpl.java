package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.impl;

import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.SimilarContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.RecommendationService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {

  private static final int CANDIDATE_POOL_SIZE = 50;

  private final ContentTagRepository contentTagRepository;
  private final ContentRepository contentRepository;

  @Override
  public List<SimilarContentDto> getSimilarContents(UUID contentId, Integer limit) {
    List<SimilarContentDto> candidates =
        contentTagRepository.findSimilarContents(contentId, PageRequest.of(0, CANDIDATE_POOL_SIZE));
    if (candidates.isEmpty()) {
      return List.of();
    }

    List<UUID> ids = new ArrayList<>(candidates.stream().map(SimilarContentDto::contentId).toList());
    ids.add(contentId);

    long totalContentCount = contentRepository.count();
    Map<UUID, Map<UUID, Double>> weightsByContent = contentTagRepository.findAllWithTagByContentIds(ids).stream()
        .collect(Collectors.groupingBy(
            ct -> ct.getId().getContentId(),
            Collectors.toMap(
                ct -> ct.getTag().getId(),
                ct -> CosineSimilarity.idf(ct.getTag().getContentCount(), totalContentCount)
            )
        ));

    Map<UUID, Double> target = weightsByContent.getOrDefault(contentId, Map.of());

    return candidates.stream()
        .map(c -> new SimilarContentDto(
            c.contentId(), c.title(), c.thumbnailUrl(),
            CosineSimilarity.compute(target, weightsByContent.getOrDefault(c.contentId(), Map.of()))
        ))
        .sorted(Comparator.comparingDouble(SimilarContentDto::score).reversed())
        .limit(limit)
        .toList();
  }
}
