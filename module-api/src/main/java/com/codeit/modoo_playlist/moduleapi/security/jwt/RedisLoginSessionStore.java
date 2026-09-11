package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginSession;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class RedisLoginSessionStore implements LoginSessionStore {

  private static final String KEY_PREFIX = "auth:session:";

  private final StringRedisTemplate redisTemplate;
  private final JsonMapper objectMapper;

  private String key(UUID userId) {
    return KEY_PREFIX + userId;
  }

  @Override
  public void register(LoginSession session) {
    Instant now = Instant.now();

    if (session.isExpired(now)) {
      throw new BaseException(ErrorCode.LOGIN_SESSION_INVALIDATED);
    }

    Duration ttl = Duration.between(
        now,
        session.expiresAt()
    );

    String value = objectMapper.writeValueAsString(session);

    redisTemplate.opsForValue().set(
        key(session.userId()),
        value,
        ttl
    );
  }

  @Override
  public Optional<LoginSession> findActive(UUID userId, UUID sid) {
    String value = redisTemplate.opsForValue().get(key(userId));

    if (value == null) {
      return Optional.empty();
    }

    LoginSession session = objectMapper.readValue(value, LoginSession.class);

//    userId와 다르면 에러.
    if (!userId.equals(session.userId())) {
      throw new BaseException(ErrorCode.LOGIN_SESSION_ID_MISMATCH);
    }

//    새로운 환경에서 로그인. 인증 만료
    if (!sid.equals(session.sid())) {
      return Optional.empty();
    }

    if (session.isExpired(Instant.now())) {
      return Optional.empty();
    }

    return Optional.of(session);
  }

  @Override
  public void rotateRefreshToken(
      UUID userId,
      UUID sid,
      String currentRefreshTokenHash,
      String newRefreshTokenHash,
      Instant newExpiresAt
  ) {
    Instant now = Instant.now();
    long ttlMillis = Duration.between(now, newExpiresAt).toMillis();

    if (ttlMillis <= 0) {
      throw new BaseException(ErrorCode.LOGIN_SESSION_INVALIDATED);
    }

    String expiresAtJson =
        objectMapper.writeValueAsString(newExpiresAt);

    Long result = redisTemplate.execute(
        ROTATE_REFRESH_TOKEN_SCRIPT,
        List.of(key(userId)),
        sid.toString(),
        currentRefreshTokenHash,
        newRefreshTokenHash,
        expiresAtJson,
        Long.toString(ttlMillis)
    );

    if (result == null) {
      throw new BaseException(ErrorCode.LOGIN_SESSION_UPDATE_FAILED);
    }

    if (result == 0L || result == -2L) {
      throw new BaseException(ErrorCode.LOGIN_SESSION_INVALIDATED);
    }

    if (result == -1L) {
      throw new BaseException(ErrorCode.INVALID_TOKEN);
    }

    if (result != 1L) {
      throw new BaseException(ErrorCode.LOGIN_SESSION_UPDATE_FAILED);
    }
  }

  @Override
  public void invalidate(UUID userId, UUID sid) {
    Long result = redisTemplate.execute(
        INVALIDATE_SESSION_SCRIPT,
        List.of(key(userId)),
        sid.toString()
    );

    if (result == null) {
      throw new BaseException(ErrorCode.LOGIN_SESSION_UPDATE_FAILED);
    }

    // 이미 삭제됐거나 새로운 로그인으로 교체된 경우
    // 로그아웃은 성공한 것으로 처리
    if (result == 0L || result == -1L) {
      return;
    }

    if (result != 1L) {
      throw new BaseException(ErrorCode.LOGIN_SESSION_UPDATE_FAILED);
    }
  }

  /*
    value로 json 값 전달
    -> value가 없으면 0 반환(세션이 없거나, 로그아웃해서 세션 삭제 등)
    -> session에 json value를 lua로 변환.
    -> argv[1]로 sid 비교. 만약 다르면 -2로 반환.
    -> argv[2]로 토큰 해시 비교. 다르면 -1 반환
    -> 목표를 수정한 session으로 set, ttl 시간 설정
    -> set 실패시 0 반환
    -> 성공하면 1반환
     */
  private static final DefaultRedisScript<Long> ROTATE_REFRESH_TOKEN_SCRIPT =
      new DefaultRedisScript<>(
          """
              local value = redis.call('GET', KEYS[1])
              
              if not value then
                return 0
              end
              
              local session = cjson.decode(value)
              
              if session.sid ~= ARGV[1] then
                return -2
              end
              
              if session.refreshTokenHash ~= ARGV[2] then
                return -1
              end
              
              session.refreshTokenHash = ARGV[3]
              session.expiresAt = cjson.decode(ARGV[4])
              
              local updated = redis.call(
                'SET',
                KEYS[1],
                cjson.encode(session),
                'PX',
                ARGV[5],
                'XX'
              )
              
              if not updated then
                return 0
              end
              
              return 1
              """,
          Long.class
      );

  /*
  userId로 value값 탐색.
  -> 없으면 0 반환
  -> session에 json valu를 lua로 변환
  -> sid가 다르면 -1 반환
  -> 정상적이면 해당 값 삭제.
   */
  private static final DefaultRedisScript<Long> INVALIDATE_SESSION_SCRIPT =
      new DefaultRedisScript<>(
          """
              local value = redis.call('GET', KEYS[1])
              
              if not value then
                return 0
              end
              
              local session = cjson.decode(value)
              
              if session.sid ~= ARGV[1] then
                return -1
              end
              
              return redis.call('DEL', KEYS[1])
              """,
          Long.class
      );
}
