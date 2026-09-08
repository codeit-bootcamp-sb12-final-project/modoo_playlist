package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.moduleapi.dto.jwt.JwtInformation;
import com.codeit.modoo_playlist.infra.store.KeyStore;
import com.codeit.modoo_playlist.infra.store.ListStore;
import com.codeit.modoo_playlist.infra.store.RedisKeyStore;
import com.codeit.modoo_playlist.infra.store.RedisListStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

// JWT 기반 세션 관리 인터페이스 구현 (Redis 버전, 서버 재시작/스케일아웃에도 세션 유지)
// - jwt:access:{accessToken}  -> JwtInformation, TTL = accessToken 만료시간
// - jwt:refresh:{refreshToken} -> JwtInformation, TTL = refreshToken 만료시간
// - jwt:user:{userId}          -> List<refreshToken> (활성 세션 큐, 오래된 순 유지)
// 개별 토큰 키는 Redis TTL로 자동 만료되므로, 스케줄러는 user 큐에 남은 "죽은" 참조만 정리한다.
@Slf4j
public class RedisJwtRegistry implements JwtRegistry<UUID> {

    private static final String USER_PREFIX = "jwt:user";
    private static final String ACCESS_PREFIX = "jwt:access";
    private static final String REFRESH_PREFIX = "jwt:refresh";

    private final int maxActiveJwtCount;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    private final KeyStore<String, JwtInformation> accessTokenStore;
    private final KeyStore<String, JwtInformation> refreshTokenStore;
    private final ListStore<UUID, String> userSessionStore;

    public RedisJwtRegistry(
            RedisTemplate<String, Object> redisTemplate,
            int maxActiveJwtCount,
            long accessTokenExpirationMs,
            long refreshTokenExpirationMs
    ) {
        this.maxActiveJwtCount = maxActiveJwtCount;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.accessTokenStore = new RedisKeyStore<>(redisTemplate, ACCESS_PREFIX);
        this.refreshTokenStore = new RedisKeyStore<>(redisTemplate, REFRESH_PREFIX);
        this.userSessionStore = new RedisListStore<>(redisTemplate, USER_PREFIX);
    }

    // 새 JWT 정보를 registry에 등록하고 maxActiveJwtCount 초과 시 가장 오래된 세션 제거
    @Override
    public void registerJwtInformation(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.getUserDto().id();

        if (userSessionStore.size(userId) >= maxActiveJwtCount) {
            String oldestRefreshToken = userSessionStore.popLeft(userId);
            if (oldestRefreshToken != null) {
                evictByRefreshToken(oldestRefreshToken);
            }
        }

        putTokenPair(jwtInformation);
        userSessionStore.pushRight(userId, jwtInformation.getRefreshToken());
        userSessionStore.expire(userId, Duration.ofMillis(refreshTokenExpirationMs));
    }

    // 특정 userId의 모든 JWT를 무효화하고 registry에서 제거
    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {
        for (String refreshToken : userSessionStore.range(userId)) {
            evictByRefreshToken(refreshToken);
        }
        userSessionStore.delete(userId);
    }

    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        return userSessionStore.size(userId) > 0;
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        return accessTokenStore.exists(accessToken);
    }

    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        return refreshTokenStore.exists(refreshToken);
    }

    // Refresh Token을 기준으로 기존 JWT 정보를 새 JWT로 교체(토큰 회전)
    @Override
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        Optional<JwtInformation> old = refreshTokenStore.get(refreshToken);
        old.ifPresent(jwtInformation -> accessTokenStore.delete(jwtInformation.getAccessToken()));
        refreshTokenStore.delete(refreshToken);

        UUID userId = newJwtInformation.getUserDto().id();
        userSessionStore.remove(userId, refreshToken);
        userSessionStore.pushRight(userId, newJwtInformation.getRefreshToken());
        userSessionStore.expire(userId, Duration.ofMillis(refreshTokenExpirationMs));

        putTokenPair(newJwtInformation);
    }

    // 실제 만료는 Redis TTL이 처리하므로 별도 정리 로직은 두지 않는다.
    @Scheduled(fixedDelay = 1000 * 60 * 5)
    @Override
    public void clearExpiredJwtInformation() {
        log.debug("clearExpiredJwtInformation: Redis TTL이 개별 토큰 만료를 자동 처리함");
    }

    private void putTokenPair(JwtInformation jwtInformation) {
        accessTokenStore.put(
                jwtInformation.getAccessToken(),
                jwtInformation,
                Duration.ofMillis(accessTokenExpirationMs)
        );
        refreshTokenStore.put(
                jwtInformation.getRefreshToken(),
                jwtInformation,
                Duration.ofMillis(refreshTokenExpirationMs)
        );
    }

    private void evictByRefreshToken(String refreshToken) {
        refreshTokenStore.get(refreshToken)
                .ifPresent(jwtInformation -> accessTokenStore.delete(jwtInformation.getAccessToken()));
        refreshTokenStore.delete(refreshToken);
    }
}
