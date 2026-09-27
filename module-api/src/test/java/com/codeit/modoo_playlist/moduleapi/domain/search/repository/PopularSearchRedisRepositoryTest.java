package com.codeit.modoo_playlist.moduleapi.domain.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.dto.PopularKeywordDto;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

@ExtendWith(MockitoExtension.class)
class PopularSearchRedisRepositoryTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ZSetOperations<String, String> zSetOperations;

  private PopularSearchRedisRepository repository;

  @BeforeEach
  void setUp() {
    repository = new PopularSearchRedisRepository(redisTemplate);
  }

  @Test
  @DisplayName("검색어 점수를 오늘 날짜의 인기 검색어에 증가시킨다")
  void incrementScore() {
    String todayKey = "search:popular:daily:" + LocalDate.now(SEOUL);

    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

    repository.incrementScore("이누야샤");

    verify(zSetOperations).incrementScore(todayKey, "이누야샤", 1);
    verify(redisTemplate).expire(todayKey, Duration.ofDays(9));
  }

  @Test
  @DisplayName("최근 검색 횟수를 합산해 인기 검색어를 반환한다")
  void getTopKeywords() {
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

    TypedTuple<String> first = tuple("이누야샤", 10.0);
    TypedTuple<String> second = tuple("인터스텔라", 7.0);
    Set<TypedTuple<String>> tuples = new LinkedHashSet<>(List.of(first, second));

    when(zSetOperations.reverseRangeWithScores(
        "search:popular:union-cache", 0, 9L)).thenReturn(tuples);

    List<PopularKeywordDto> result = repository.getTopKeywords(7, 10);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).keyword()).isEqualTo("이누야샤");
    assertThat(result.get(0).count()).isEqualTo(10L);
    assertThat(result.get(1).keyword()).isEqualTo("인터스텔라");
    assertThat(result.get(1).count()).isEqualTo(7L);

    verify(zSetOperations).unionAndStore(
        anyString(),
        org.mockito.ArgumentMatchers.<List<String>>any(),
        eq("search:popular:union-cache"));
    verify(redisTemplate).expire("search:popular:union-cache", Duration.ofSeconds(60));
  }

  @Test
  @DisplayName("인기 검색어 조회 결과가 없으면 빈 목록을 반환한다")
  void returnEmptyWhenTopKeywordsAreMissing() {
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    when(zSetOperations.reverseRangeWithScores(
        "search:popular:union-cache", 0, 9L)).thenReturn(null);

    List<PopularKeywordDto> result = repository.getTopKeywords(7, 10);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("검색 횟수가 없으면 0으로 반환한다")
  void convertNullScoreToZero() {
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

    TypedTuple<String> tuple = tuple("이누야샤", null);
    when(zSetOperations.reverseRangeWithScores(
        "search:popular:union-cache", 0, 9L)).thenReturn(Set.of(tuple));

    List<PopularKeywordDto> result = repository.getTopKeywords(7, 10);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).keyword()).isEqualTo("이누야샤");
    assertThat(result.get(0).count()).isZero();
  }

  @SuppressWarnings("unchecked")
  private TypedTuple<String> tuple(String keyword, Double score) {
    TypedTuple<String> tuple = mock(TypedTuple.class);
    when(tuple.getValue()).thenReturn(keyword);
    when(tuple.getScore()).thenReturn(score);
    return tuple;
  }
}
