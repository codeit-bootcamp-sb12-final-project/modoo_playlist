package com.codeit.modoo_playlist.moduleapi.domain.search.event;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.modoo_playlist.moduleapi.domain.search.repository.PopularSearchRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchEventListenerTest {

  @Mock
  private PopularSearchRedisRepository popularSearchRedisRepository;

  @InjectMocks
  private SearchEventListener searchEventListener;

  @Test
  @DisplayName("검색 이벤트를 받으면 정규화된 검색어의 점수를 증가시킨다")
  void handleSearchEvent() {
    SearchExecutedEvent event = new SearchExecutedEvent("  이누야샤  ");

    searchEventListener.handle(event);

    verify(popularSearchRedisRepository).incrementScore("이누야샤");
  }

  @Test
  @DisplayName("공백 검색어는 인기 검색어에 반영하지 않는다")
  void ignoreBlankKeyword() {
    SearchExecutedEvent event = new SearchExecutedEvent("   ");

    searchEventListener.handle(event);

    verify(popularSearchRedisRepository, never()).incrementScore(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("최대 길이를 초과한 검색어는 인기 검색어에 반영하지 않는다")
  void ignoreTooLongKeyword() {
    SearchExecutedEvent event = new SearchExecutedEvent("가".repeat(51));

    searchEventListener.handle(event);

    verify(popularSearchRedisRepository, never()).incrementScore(org.mockito.ArgumentMatchers.any());
  }
}
