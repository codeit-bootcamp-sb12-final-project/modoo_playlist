package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import com.codeit.modoo_playlist.core.global.common.util.KeywordNormalizer;
import com.codeit.modoo_playlist.moduleapi.domain.search.event.SearchExecutedEvent;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.PopularSearchRedisRepository;
import com.codeit.modoo_playlist.moduleapi.dto.PopularKeywordDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopularSearchService {

  private static final int MIN_POPULAR_KEYWORD_LENGTH = 2;
  private static final int AGGREGATION_PERIOD_DAYS = 7;
  private static final int TOP_KEYWORD_LIMIT = 10;

  private final PopularSearchRedisRepository popularSearchRedisRepository;
  private final ContentSearchRepository contentSearchRepository;
  private final ApplicationEventPublisher eventPublisher;

  public List<PopularKeywordDto> getPopularKeywords() {
    return popularSearchRedisRepository.getTopKeywords(AGGREGATION_PERIOD_DAYS, TOP_KEYWORD_LIMIT);
  }

  public void recordSearch(String rawKeyword) {
    String keyword = KeywordNormalizer.normalize(rawKeyword);

    if (!KeywordNormalizer.isValidLength(keyword)) {
      return;
    }

    if (KeywordNormalizer.codePointLength(keyword) < MIN_POPULAR_KEYWORD_LENGTH
        && contentSearchRepository.findFirstByNormalizedTitle(keyword).isEmpty()) {
      return;
    }

    eventPublisher.publishEvent(new SearchExecutedEvent(keyword));
  }
}
