package com.codeit.modoo_playlist.moduleapi.domain.search.repository;

import com.codeit.modoo_playlist.moduleapi.dto.PopularKeywordDto;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PopularSearchRedisRepository {

  private static final String DAILY_KEY_PREFIX = "search:popular:daily:";
  private static final String UNION_CACHE_KEY = "search:popular:union-cache";
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

  private static final Duration DAILY_KEY_TTL = Duration.ofDays(9);

  private static final Duration UNION_CACHE_TTL = Duration.ofSeconds(60);

  private final StringRedisTemplate redisTemplate;

  public void incrementScore(String normalizedKeyword) {
    String todayKey = dailyKey(LocalDate.now(SEOUL));
    redisTemplate.opsForZSet().incrementScore(todayKey, normalizedKeyword, 1);
    redisTemplate.expire(todayKey, DAILY_KEY_TTL);
  }

  // 최근 N일 검색 횟수 합산 후 상위 인기 검색어 조회
  public List<PopularKeywordDto> getTopKeywords(int days, int limit) {
    List<String> dailyKeys = recentDailyKeys(days);

    redisTemplate.opsForZSet()
        .unionAndStore(dailyKeys.get(0), dailyKeys.subList(1, dailyKeys.size()), UNION_CACHE_KEY);
    redisTemplate.expire(UNION_CACHE_KEY, UNION_CACHE_TTL);

    Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet()
        .reverseRangeWithScores(UNION_CACHE_KEY, 0, limit - 1L);

    if (tuples == null) {
      return List.of();
    }

    return tuples.stream()
        .map(tuple -> new PopularKeywordDto(
            tuple.getValue(),
            tuple.getScore() == null ? 0L : tuple.getScore().longValue()))
        .collect(Collectors.toList());
  }

  // 최근 N일치 daily key 문자열 리스트 생성
  private List<String> recentDailyKeys(int days) {
    LocalDate today = LocalDate.now(SEOUL);
    return IntStream.range(0, days)
        .mapToObj(offset -> dailyKey(today.minusDays(offset)))
        .collect(Collectors.toList());
  }

  // 특정 날짜에 대응하는 Redis daily key 문자열 생성
  private String dailyKey(LocalDate date) {
    return DAILY_KEY_PREFIX + date.format(DATE_FORMAT);
  }

}
