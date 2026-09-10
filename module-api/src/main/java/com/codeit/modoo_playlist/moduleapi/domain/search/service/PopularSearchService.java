package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import com.codeit.modoo_playlist.moduleapi.domain.search.repository.PopularSearchRedisRepository;
import com.codeit.modoo_playlist.moduleapi.dto.PopularKeywordDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopularSearchService {

  // 인기 검색어 집계 기간
  private static final int AGGREGATION_PERIOD_DAYS = 7;

  // 인기 검색어 노출 개수
  private static final int TOP_KEYWORD_LIMIT = 10;

  private final PopularSearchRedisRepository popularSearchRedisRepository;

  // 최근 7일 기준 상위 10개 인기 검색어 조회
  public List<PopularKeywordDto> getPopularKeywords() {
    return popularSearchRedisRepository.getTopKeywords(AGGREGATION_PERIOD_DAYS, TOP_KEYWORD_LIMIT);
  }

}
