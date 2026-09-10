package com.codeit.modoo_playlist.moduleapi.domain.search.event;

import com.codeit.modoo_playlist.core.global.common.util.KeywordNormalizer;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.PopularSearchRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchEventListener {

  private final PopularSearchRedisRepository popularSearchRedisRepository;

  // 검색어 정규화 및 유효성 확인 → 인기 검색어 점수 반영
  @Async("searchAsyncExecutor")
  @EventListener
  public void handle(SearchExecutedEvent event) {
    String normalized = KeywordNormalizer.normalize(event.rawKeyword());

    if (!KeywordNormalizer.isValidLength(normalized)) {
      return;
    }

    popularSearchRedisRepository.incrementScore(normalized);
  }

}
