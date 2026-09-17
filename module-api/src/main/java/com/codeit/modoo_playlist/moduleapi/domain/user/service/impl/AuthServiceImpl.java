package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.AuthService;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.TemporaryPasswordSender;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginIssueResult;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginSession;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.TokenRefreshResult;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.codeit.modoo_playlist.moduleapi.security.UserDetailsService;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import com.codeit.modoo_playlist.moduleapi.security.jwt.LoginSessionStore;
import com.codeit.modoo_playlist.moduleapi.security.jwt.RefreshTokenHasher;
import com.nimbusds.jose.JOSEException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final JwtTokenProvider tokenProvider;
  private final LoginSessionStore loginSessionStore;
  private final UserDetailsService userDetailsService;
  private final RefreshTokenHasher refreshTokenHasher;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TemporaryPasswordSender temporaryPasswordSender;
  private final Clock clock;
  private final UserMapper userMapper;

  @Value("${module-api.auth.temporary-password.value}")
  private String temporaryPassword;

  @Value("${module-api.auth.temporary-password.expiration}")
  private Duration temporaryPasswordExpiration;

  @Value("${module-api.jwt.refresh-token.expiration-ms}")
  private long refreshTokenExpirationMs;

  @Value("${module-api.jwt.refresh-token.max-expiration-ms}")
  private long maxRefreshTokenExpirationMs;

  @Override
  @Transactional
  public void resetPassword(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    if (user.getPassword() == null) {
      throw new BaseException(ErrorCode.PASSWORD_RESET_NOT_SUPPORTED);
    }

    if (user.isLocked()) {
      throw new BaseException(ErrorCode.USER_ACCOUNT_LOCKED);
    }

    String encodedTemporaryPassword = passwordEncoder.encode(temporaryPassword);
    Instant expiresAt = clock.instant().plus(temporaryPasswordExpiration);

    user.issueTemporaryPassword(encodedTemporaryPassword, expiresAt);
    userRepository.saveAndFlush(user);
    temporaryPasswordSender.send(user.getEmail(), temporaryPassword, expiresAt);
  }

  //  잠금 -> 잠금 및 역할 재 확인 -> 토큰 생성 및 세션 등록 -> 종료
  @Transactional
  @Override
  public LoginIssueResult issueLogin(UUID userId) {
    User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    // 최초 인증 이후 관리자가 계정을 잠갔는지 다시 확인
    if (user.isLocked()) {
      throw new BaseException(ErrorCode.USER_ACCOUNT_LOCKED);
    }

    UserDto currentUserDto = userMapper.toDto(user);

    UserDetails currentUserDetails = new UserDetails(
        currentUserDto,
        user.getPassword()
    );

    Instant createdAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);

    Instant expiresAt = createdAt.plusMillis(refreshTokenExpirationMs)
        .truncatedTo(ChronoUnit.SECONDS);

    UUID sid = UUID.randomUUID();

    String accessToken;
    String refreshToken;

    try {
      accessToken = tokenProvider.generateAccessToken(
          currentUserDetails,
          sid,
          expiresAt
      );

      refreshToken = tokenProvider.generateRefreshToken(
          currentUserDetails,
          sid,
          expiresAt
      );

    } catch (JOSEException e) {
      throw new BaseException(ErrorCode.TOKEN_GENERATION_FAILED, e);
    }

    LoginSession session = new LoginSession(
        sid,
        userId,
        refreshTokenHasher.hash(refreshToken),
        createdAt,
        expiresAt
    );

    // 사용자 행 잠금을 보유한 상태에서 Redis 세션 등록
    loginSessionStore.register(session);

    return new LoginIssueResult(
        currentUserDto,
        accessToken,
        refreshToken,
        expiresAt
    );
  }

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
