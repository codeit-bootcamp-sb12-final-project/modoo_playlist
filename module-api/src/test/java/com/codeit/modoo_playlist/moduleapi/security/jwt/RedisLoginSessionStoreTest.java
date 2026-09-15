package com.codeit.modoo_playlist.moduleapi.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginSession;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

//  Spring 웹, JPA 없이 실제 Redis에서 저장·조회·Lua 스크립트를 검증한다.
class RedisLoginSessionStoreTest {

  private static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse(
          "redis/redis-stack-server"))
          .withExposedPorts(6379);
  private static LettuceConnectionFactory connection;
  private static StringRedisTemplate redis;
  private static RedisLoginSessionStore store;
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private LoginSession session;

  @BeforeAll
  static void startRedis() {
    REDIS.start();
    connection = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    connection.afterPropertiesSet();
    connection.start();
    redis = new StringRedisTemplate(connection);
    store = new RedisLoginSessionStore(redis, JSON);
  }

  @AfterAll
  static void stopRedis() {
    if (connection != null) {
      connection.destroy();
    }
    REDIS.stop();
  }

  @BeforeEach
  void setUp() {
    session = new LoginSession(UUID.randomUUID(), UUID.randomUUID(), "old-hash",
        Instant.now(), Instant.now().plusSeconds(600));
  }

  @Test
  @DisplayName("세션 저장 시 내용과 TTL을 기록.")
  void registerAndRead() {
    store.register(session);

    assertThat(store.findActive(session.userId(), session.sid())).contains(session);
    assertThat(redis.getExpire(key(), TimeUnit.SECONDS)).isBetween(1L, 600L);
  }

  @Test
  @DisplayName("없거나 다른 sid면 저장")
  void missingOrDifferentSid() {
    assertThat(store.findActive(session.userId(), session.sid())).isEmpty();

    store.register(session);

    assertThat(store.findActive(session.userId(), UUID.randomUUID())).isEmpty();
  }

  @Test
  @DisplayName("세션 무효화")
  void expiredSession() {
    LoginSession expired = new LoginSession(session.sid(), session.userId(), "hash",
        Instant.now().minusSeconds(600), Instant.now().minusSeconds(1));

    assertError(() -> store.register(expired), ErrorCode.LOGIN_SESSION_INVALIDATED);
    assertThat(redis.hasKey(key())).isFalse();

    redis.opsForValue().set(key(), JSON.writeValueAsString(expired));

    assertThat(store.findActive(session.userId(), session.sid())).isEmpty();
  }

  @Test
  @DisplayName("일치하지 않는 유저")
  void mismatchedStoredUser() {
    LoginSession other = new LoginSession(session.sid(), UUID.randomUUID(), "hash",
        session.createdAt(), session.expiresAt());

    redis.opsForValue().set(key(), JSON.writeValueAsString(other));

    assertError(() -> store.findActive(session.userId(), session.sid()),
        ErrorCode.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("토큰 교체 시 해시와 만료시간을 갱신하고 이전 해시 재사용은 차단.")
  void rotate() {
    store.register(session);

    Instant expires = Instant.now().plusSeconds(1200);

    store.rotateRefreshToken(session.userId(), session.sid(), "old-hash", "new-hash", expires);

    LoginSession renewed = store.findActive(session.userId(), session.sid()).orElseThrow();

    assertThat(renewed.refreshTokenHash()).isEqualTo("new-hash");
    assertThat(renewed.createdAt()).isEqualTo(session.createdAt());
    assertThat(renewed.expiresAt()).isEqualTo(expires);
    assertThat(redis.getExpire(key(), TimeUnit.SECONDS)).isBetween(601L, 1200L);
    assertError(
        () -> store.rotateRefreshToken(session.userId(), session.sid(), "old-hash", "bad", expires),
        ErrorCode.INVALID_TOKEN);
    assertThat(store.findActive(session.userId(), session.sid())).contains(renewed);
  }

  @Test
  @DisplayName("잘못된 sid는 교체 안함.")
  void wrongSidCannotRotate() {
    store.register(session);

    assertError(
        () -> store.rotateRefreshToken(session.userId(), UUID.randomUUID(), "old-hash", "new",
            session.expiresAt()),
        ErrorCode.LOGIN_SESSION_INVALIDATED);
    assertThat(store.findActive(session.userId(), session.sid())).contains(session);
  }

  @Test
  @DisplayName("sid 없으면 교체 안함.")
  void missingSessionCannotRotate() {
    assertError(() -> store.rotateRefreshToken(session.userId(), session.sid(), "old-hash", "new",
            session.expiresAt()),
        ErrorCode.LOGIN_SESSION_INVALIDATED);
    assertThat(redis.hasKey(key())).isFalse();
  }

  @Test
  @DisplayName("만료된 세션 교체 안함")
  void expiredRotationDoesNotChangeSession() {
    store.register(session);

    assertError(() -> store.rotateRefreshToken(session.userId(), session.sid(), "old-hash", "new",
            Instant.now().minusSeconds(1)),
        ErrorCode.LOGIN_SESSION_INVALIDATED);
    assertThat(store.findActive(session.userId(), session.sid())).contains(session);
  }

  @Test
  @DisplayName("최근 로그인 계정이 sid 소유권.")
  void replacingSessionAndStaleLogout() {
    store.register(session);

    LoginSession replacement = new LoginSession(UUID.randomUUID(), session.userId(), "new",
        Instant.now(), session.expiresAt());

    store.register(replacement);
    store.invalidate(session.userId(), session.sid());

    assertThat(store.findActive(session.userId(), session.sid())).isEmpty();
    assertThat(store.findActive(replacement.userId(), replacement.sid())).contains(replacement);
  }

  @Test
  void invalidateIsIdempotent() {
    store.register(session);
    store.invalidate(session.userId(), session.sid());
    store.invalidate(session.userId(), session.sid());

    assertThat(redis.hasKey(key())).isFalse();
    assertThat(store.findActive(session.userId(), session.sid())).isEmpty();
  }

  private String key() {
    return "auth:session:" + session.userId();
  }

  private void assertError(Runnable action, ErrorCode code) {
    assertThatThrownBy(action::run).isInstanceOfSatisfying(BaseException.class,
        e -> assertThat(e.getErrorCode()).isEqualTo(code));
  }
}
