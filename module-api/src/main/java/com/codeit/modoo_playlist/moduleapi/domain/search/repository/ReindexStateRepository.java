package com.codeit.modoo_playlist.moduleapi.domain.search.repository;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReindexStateRepository {

  private static final String REINDEX_LOCK_KEY = "search:reindex:lock";
  private static final String REINDEX_TARGET_KEY = "search:reindex:target";
  private static final String CHANGED_CONTENT_IDS_KEY = "search:reindex:changed-content-ids";
  private static final String SWITCHING_KEY = "search:reindex:switching";
  private static final Duration REINDEX_TTL = Duration.ofMinutes(30);

  private final StringRedisTemplate redisTemplate;

  public boolean start(String targetIndex) {
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(REINDEX_LOCK_KEY, targetIndex, REINDEX_TTL);

    if (!Boolean.TRUE.equals(acquired)) {
      return false;
    }

    redisTemplate.opsForValue().set(REINDEX_TARGET_KEY, targetIndex, REINDEX_TTL);
    return true;
  }

  public boolean isReindexing() {
    return Boolean.TRUE.equals(redisTemplate.hasKey(REINDEX_LOCK_KEY));
  }

  public String getTargetIndex() {
    return redisTemplate.opsForValue().get(REINDEX_TARGET_KEY);
  }

  public void recordChangedContent(UUID contentId) {
    if (!isReindexing()) {
      return;
    }

    redisTemplate.opsForSet().add(CHANGED_CONTENT_IDS_KEY, contentId.toString());
    redisTemplate.expire(CHANGED_CONTENT_IDS_KEY, REINDEX_TTL);
  }

  public String popChangedContentId() {
    return redisTemplate.opsForSet().pop(CHANGED_CONTENT_IDS_KEY);
  }

  public boolean startSwitching() {
    Boolean started = redisTemplate.opsForValue()
        .setIfAbsent(SWITCHING_KEY, "true", REINDEX_TTL);

    return Boolean.TRUE.equals(started);
  }

  public boolean isSwitching() {
    return Boolean.TRUE.equals(redisTemplate.hasKey(SWITCHING_KEY));
  }

  public void finish() {
    redisTemplate.delete(CHANGED_CONTENT_IDS_KEY);
    redisTemplate.delete(SWITCHING_KEY);
    redisTemplate.delete(REINDEX_TARGET_KEY);
    redisTemplate.delete(REINDEX_LOCK_KEY);
  }

}
