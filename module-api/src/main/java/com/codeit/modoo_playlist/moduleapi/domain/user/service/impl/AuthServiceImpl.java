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
  private int refreshTokenExpirationMs;

  @Value("${module-api.jwt.refresh-token.max-expiration-ms}")
  private long maxRefreshTokenExpirationMs;

  @Override
  public TokenRefreshResult refresh(String refreshToken) {
//    토큰 유무 체크
    if (!StringUtils.hasText(refreshToken)) {
      throw new BaseException(ErrorCode.INVALID_TOKEN);
    }

//    토큰 검증
    if (!tokenProvider.validateRefreshToken(refreshToken)) {
      throw new BaseException(ErrorCode.INVALID_TOKEN);
    }

    UUID sid = tokenProvider.getSid(refreshToken);
    UUID tokenUserId = tokenProvider.getUserId(refreshToken);
    String email = tokenProvider.getUsernameFromToken(refreshToken);

//    활성 유저인지 체크
    LoginSession session =
        loginSessionStore.findActive(tokenUserId, sid)
            .orElseThrow(() -> new BaseException(ErrorCode.LOGIN_SESSION_INVALIDATED));

//    사용자 판단을 다음 세개로 확인
//    토큰 user == 세션 user == DB에서 조회한 사용자
//    redis에 저장된 유저와 요청 유저 같은지 비교
    if (!tokenUserId.equals(session.userId())) {
      throw new BaseException(ErrorCode.LOGIN_SESSION_ID_MISMATCH);
    }

    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

    if (!tokenUserId.equals(userDetails.getUserDto().id())) {
      throw new BaseException(ErrorCode.LOGIN_SESSION_ID_MISMATCH);
    }

//    여기서도 동시성이 발생할 가능성이 존재.
//    그래서 비교 후 토큰 교체가 아니라, 비교와 동시에 토큰 교체로 진행
//    그래서 토큰 부터 먼저 생성한다.
    String currentRefreshHash = refreshTokenHasher.hash(refreshToken);

    Instant maxExpiredAt = session.createdAt().plusMillis(maxRefreshTokenExpirationMs)
        .truncatedTo(ChronoUnit.SECONDS);

    Instant renewedExpiredAt = Instant.now().plusMillis(refreshTokenExpirationMs)
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
      throw new BaseException(ErrorCode.TOKEN_GENERATION_FAILED);
    }

    String newRefreshTokenHash = refreshTokenHasher.hash(newRefreshToken);

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
  }
}
