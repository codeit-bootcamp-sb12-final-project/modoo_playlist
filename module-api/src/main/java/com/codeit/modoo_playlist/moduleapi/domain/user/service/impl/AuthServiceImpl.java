package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.AuthService;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginSession;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.TokenRefreshResult;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.codeit.modoo_playlist.moduleapi.security.UserDetailsService;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import com.codeit.modoo_playlist.moduleapi.security.jwt.LoginSessionStore;
import com.codeit.modoo_playlist.moduleapi.security.jwt.RefreshTokenHasher;
import com.nimbusds.jose.JOSEException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final JwtTokenProvider tokenProvider;
  private final LoginSessionStore loginSessionStore;
  private final UserDetailsService userDetailsService;
  private final RefreshTokenHasher refreshTokenHasher;

  @Value("${module-api.jwt.refresh-token.expiration-ms}")
  private long refreshTokenExpirationMs;

  @Value("${module-api.jwt.refresh-token.max-expiration-ms}")
  private long maxRefreshTokenExpirationMs;

  @Override
  public TokenRefreshResult refresh(String refreshToken) {
    try {
      // 토큰 유무 체크
      if (!StringUtils.hasText(refreshToken)) {
        throw new BaseException(ErrorCode.INVALID_TOKEN);
      }

      // refresh token 검증
      if (!tokenProvider.validateRefreshToken(refreshToken)) {
        throw new BaseException(ErrorCode.INVALID_TOKEN);
      }

      UUID sid = tokenProvider.getSid(refreshToken);
      UUID tokenUserId = tokenProvider.getUserId(refreshToken);
      String email = tokenProvider.getUsernameFromToken(refreshToken);

      // Redis에서 활성 로그인 세션 조회
      LoginSession session =
          loginSessionStore.findActive(tokenUserId, sid)
              .orElseThrow(() ->
                  new BaseException(
                      ErrorCode.LOGIN_SESSION_INVALIDATED
                  ));

      // 토큰 사용자와 Redis 세션 사용자 비교
      if (!tokenUserId.equals(session.userId())) {
        throw new BaseException(ErrorCode.INVALID_TOKEN);
      }

      UserDetails userDetails;

      try {
        userDetails = userDetailsService.loadUserByUsername(email);
      } catch (UsernameNotFoundException e) {
        throw new BaseException(
            ErrorCode.INVALID_TOKEN,
            e
        );
      }

      // 토큰 사용자와 DB 사용자 비교
      if (!tokenUserId.equals(userDetails.getUserDto().id())) {
        throw new BaseException(ErrorCode.INVALID_TOKEN);
      }

//      유저 잠금 방어 코드
      if (!userDetails.isAccountNonLocked()) {
        throw new BaseException(ErrorCode.USER_ACCOUNT_LOCKED);
      }

      String currentRefreshHash =
          refreshTokenHasher.hash(refreshToken);

      Instant maxExpiredAt =
          session.createdAt()
              .plusMillis(maxRefreshTokenExpirationMs)
              .truncatedTo(ChronoUnit.SECONDS);

      Instant renewedExpiredAt =
          Instant.now()
              .plusMillis(refreshTokenExpirationMs)
              .truncatedTo(ChronoUnit.SECONDS);

      Instant expiredAt =
          maxExpiredAt.isAfter(renewedExpiredAt)
              ? renewedExpiredAt
              : maxExpiredAt;

      String newAccessToken;
      String newRefreshToken;

      try {
        newAccessToken = tokenProvider.generateAccessToken(
            userDetails,
            sid,
            expiredAt
        );

        newRefreshToken = tokenProvider.generateRefreshToken(
            userDetails,
            sid,
            expiredAt
        );

      } catch (JOSEException e) {
        throw new BaseException(
            ErrorCode.TOKEN_GENERATION_FAILED,
            e
        );
      }

      String newRefreshTokenHash =
          refreshTokenHasher.hash(newRefreshToken);

      // Redis에서 refresh token을 원자적으로 교체
      loginSessionStore.rotateRefreshToken(
          tokenUserId,
          sid,
          currentRefreshHash,
          newRefreshTokenHash,
          expiredAt
      );

      return new TokenRefreshResult(
          userDetails.getUserDto(),
          newAccessToken,
          newRefreshToken,
          expiredAt
      );

    } catch (DataAccessException e) {
      throw new BaseException(
          ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE,
          e
      );
    }
  }
}