package com.codeit.modoo_playlist.moduleapi.domain.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ReindexStateRepositoryTest {

  private static final String REINDEX_LOCK_KEY = "search:reindex:lock";
  private static final String REINDEX_TARGET_KEY = "search:reindex:target";
  private static final String CHANGED_CONTENT_IDS_KEY = "search:reindex:changed-content-ids";
  private static final String SWITCHING_KEY = "search:reindex:switching";
  private static final Duration REINDEX_TTL = Duration.ofMinutes(30);

  private static final UUID CONTENT_ID = UUID.fromString("019ed8a0-0000-7000-9300-000000000001");

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @Mock
  private SetOperations<String, String> setOperations;

  private ReindexStateRepository repository;

  @BeforeEach
  void setUp() {
    repository = new ReindexStateRepository(redisTemplate);
  }

  @Test
  @DisplayName("재색인 lock 획득에 성공하면 대상 인덱스를 저장한다")
  void startReindex() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(REINDEX_LOCK_KEY, "contents_v2", REINDEX_TTL)).thenReturn(true);

    boolean result = repository.start("contents_v2");

    assertThat(result).isTrue();
    verify(valueOperations).set(REINDEX_TARGET_KEY, "contents_v2", REINDEX_TTL);
  }

  @Test
  @DisplayName("재색인 lock 획득에 실패하면 대상 인덱스를 저장하지 않는다")
  void failToStartReindex() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(REINDEX_LOCK_KEY, "contents_v2", REINDEX_TTL)).thenReturn(false);

    boolean result = repository.start("contents_v2");

    assertThat(result).isFalse();
    verify(valueOperations, never()).set(REINDEX_TARGET_KEY, "contents_v2", REINDEX_TTL);
  }

  @Test
  @DisplayName("재색인 진행 여부를 반환한다")
  void checkReindexing() {
    when(redisTemplate.hasKey(REINDEX_LOCK_KEY)).thenReturn(true);

    assertThat(repository.isReindexing()).isTrue();
  }

  @Test
  @DisplayName("재색인 대상 인덱스를 반환한다")
  void getTargetIndex() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(REINDEX_TARGET_KEY)).thenReturn("contents_v2");

    assertThat(repository.getTargetIndex()).isEqualTo("contents_v2");
  }

  @Test
  @DisplayName("재색인 중 변경된 콘텐츠 ID를 기록한다")
  void recordChangedContent() {
    when(redisTemplate.hasKey(REINDEX_LOCK_KEY)).thenReturn(true);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);

    repository.recordChangedContent(CONTENT_ID);

    verify(setOperations).add(CHANGED_CONTENT_IDS_KEY, CONTENT_ID.toString());
    verify(redisTemplate).expire(CHANGED_CONTENT_IDS_KEY, REINDEX_TTL);
  }

  @Test
  @DisplayName("재색인 중이 아니면 변경된 콘텐츠 ID를 기록하지 않는다")
  void skipChangedContentWhenNotReindexing() {
    when(redisTemplate.hasKey(REINDEX_LOCK_KEY)).thenReturn(false);

    repository.recordChangedContent(CONTENT_ID);

    verify(redisTemplate, never()).opsForSet();
    verify(redisTemplate, never()).expire(CHANGED_CONTENT_IDS_KEY, REINDEX_TTL);
  }

  @Test
  @DisplayName("기록된 변경 콘텐츠 ID를 하나 꺼낸다")
  void popChangedContentId() {
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.pop(CHANGED_CONTENT_IDS_KEY)).thenReturn(CONTENT_ID.toString());

    String result = repository.popChangedContentId();

    assertThat(result).isEqualTo(CONTENT_ID.toString());
  }

  @Test
  @DisplayName("인덱스 전환 상태 시작에 성공한다")
  void startSwitching() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(SWITCHING_KEY, "true", REINDEX_TTL)).thenReturn(true);

    assertThat(repository.startSwitching()).isTrue();
  }

  @Test
  @DisplayName("인덱스 전환 여부를 반환한다")
  void checkSwitching() {
    when(redisTemplate.hasKey(SWITCHING_KEY)).thenReturn(true);

    assertThat(repository.isSwitching()).isTrue();
  }

  @Test
  @DisplayName("재색인 완료 시 관련 상태를 모두 삭제한다")
  void finishReindex() {
    repository.finish();

    verify(redisTemplate).delete(CHANGED_CONTENT_IDS_KEY);
    verify(redisTemplate).delete(SWITCHING_KEY);
    verify(redisTemplate).delete(REINDEX_TARGET_KEY);
    verify(redisTemplate).delete(REINDEX_LOCK_KEY);
  }
}
