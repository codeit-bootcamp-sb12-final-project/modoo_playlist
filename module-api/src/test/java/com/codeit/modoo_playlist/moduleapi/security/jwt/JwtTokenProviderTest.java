package com.codeit.modoo_playlist.moduleapi.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.config.AuthCookieProperties;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JwtTokenProviderTest {

  private static final String ACCESS_SECRET = "test-access-secret-at-least-32-bytes-long";
  private static final String REFRESH_SECRET = "test-refresh-secret-at-least-32-bytes-long";
  private static final String EMAIL = "test@example.com";
  private static final String ISSUER = "test";

  private JwtTokenProvider provider;
  private UUID userId;
  private UUID sid;
  private UserDetails user;

  @BeforeEach
  void setUp() throws Exception {
    userId = UUID.randomUUID();
    sid = UUID.randomUUID();
    user = new UserDetails(new UserDto(userId, EMAIL, "test", null, UserRole.USER, false,
        Instant.now()), "encoded-password");
    provider = new JwtTokenProvider(ACCESS_SECRET, 600_000, REFRESH_SECRET, 3_600_000,
        ISSUER, new AuthCookieProperties(true, "Strict", "/api/auth"));
  }

  @Test
  @DisplayName("서명된 토큰에서 사용자 및 세션을 추출. access/refresh 용도를 구분.")
  void roundTrip() throws Exception {
    Instant expiry = Instant.now().plusSeconds(3600);

    String access = provider.generateAccessToken(user, sid, expiry);
    String refresh = provider.generateRefreshToken(user, sid, expiry);

    assertThat(provider.validateAccessToken(access)).isTrue();
    assertThat(provider.validateRefreshToken(refresh)).isTrue();
    assertThat(provider.validateAccessToken(refresh)).isFalse();
    assertThat(provider.validateRefreshToken(access)).isFalse();
    assertThat(provider.getUserId(access)).isEqualTo(userId);
    assertThat(provider.getSid(access)).isEqualTo(sid);
    assertThat(provider.getUsernameFromToken(access)).isEqualTo(EMAIL);
  }

  @Test
  @DisplayName("access 토큰의 만료 < 세션 만료")
  void accessExpiryCappedBySession() throws Exception {
    Instant expiry = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.SECONDS);

    String access = provider.generateAccessToken(user, sid, expiry);

    assertThat(SignedJWT.parse(access).getJWTClaimsSet().getExpirationTime().toInstant()).isEqualTo(
        expiry);
  }

  @ParameterizedTest
  @ValueSource(strings = {"access", "refresh"})
  @DisplayName("만료된 토큰은 거부.")
  void rejectsExpiredToken(String type) throws Exception {
    JWTClaimsSet claims = validClaims(type).expirationTime(Date.from(Instant.now().minusSeconds(1)))
        .build();

    String token = sign(claims, type.equals("access") ? ACCESS_SECRET : REFRESH_SECRET);

    assertThat(type.equals("access") ? provider.validateAccessToken(token)
        : provider.validateRefreshToken(token)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"issuer", "subject", "userId", "sid", "type", "jti", "futureIssuedAt",
      "expiration"})
  @DisplayName("서명이 유효해도 필수 claim이 잘못되면 거부.")
  void rejectsInvalidClaims(String field) throws Exception {
    JWTClaimsSet.Builder claims = validClaims("access");

    switch (field) {
      case "issuer" -> claims.issuer("other");
      case "subject" -> claims.subject("");
      case "userId", "sid" -> claims.claim(field, "not-a-uuid");
      case "type" -> claims.claim("type", "refresh");
      case "jti" -> claims.jwtID(null);
      case "futureIssuedAt" -> claims.issueTime(Date.from(Instant.now().plusSeconds(120)));
      case "expiration" -> claims.expirationTime(null);
      default -> throw new AssertionError(field);
    }

    assertThat(provider.validateAccessToken(sign(claims.build(), ACCESS_SECRET))).isFalse();
  }

  @Test
  @DisplayName("다른 키로 서명한 토큰 및 파싱 불가능한 토큰을 거부.")
  void rejectsInvalidSignature() throws Exception {
    assertThat(provider.validateAccessToken(
        sign(validClaims("access").build(), REFRESH_SECRET))).isFalse();
    assertThat(provider.validateAccessToken("not-a-jwt")).isFalse();
    assertThat(provider.validateRefreshToken(null)).isFalse();
  }

  //  환경변수로 보안 방법 선택.
  @Test
  @DisplayName("쿠키 생성과 삭제에 동일한 보안 속성을 적용.")
  void cookiePolicy() {
    Cookie issued = provider.generateRefreshTokenCookie("token", Instant.now().plusSeconds(120));
    Cookie removed = provider.generateRefreshTokenExpirationCookie();

    for (Cookie cookie : new Cookie[]{issued, removed}) {
      assertThat(cookie.getName()).isEqualTo("REFRESH_TOKEN");
      assertThat(cookie.isHttpOnly()).isTrue();
      assertThat(cookie.getSecure()).isTrue();
      assertThat(cookie.getPath()).isEqualTo("/api/auth");
      assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
    }

    assertThat(issued.getMaxAge()).isBetween(1, 120);
    assertThat(removed.getMaxAge()).isZero();
    assertThat(removed.getValue()).isEmpty();
    assertThatThrownBy(
        () -> provider.generateRefreshTokenCookie("token", Instant.now().minusSeconds(1)))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LOGIN_SESSION_INVALIDATED));
  }

  @Test
  @DisplayName("이미 만료된 세션에 토큰을 발급하지 않는다")
  void noTokensForExpiredSession() {
    Instant expired = Instant.now().minusSeconds(1);

    assertThatThrownBy(() -> provider.generateAccessToken(user, sid, expired))
        .isInstanceOfSatisfying(
            BaseException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_SESSION_INVALIDATED)
        );

    assertThatThrownBy(() -> provider.generateRefreshToken(user, sid, expired))
        .isInstanceOfSatisfying(
            BaseException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_SESSION_INVALIDATED)
        );
  }

  private JWTClaimsSet.Builder validClaims(String type) {
    return new JWTClaimsSet.Builder().issuer(ISSUER).subject(EMAIL)
        .claim("userId", userId.toString()).claim("sid", sid.toString()).claim("type", type)
        .jwtID(UUID.randomUUID().toString()).issueTime(Date.from(Instant.now().minusSeconds(60)))
        .expirationTime(Date.from(Instant.now().plusSeconds(600)));
  }

  private String sign(JWTClaimsSet claims, String secret) throws Exception {
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(new MACSigner(secret));
    return jwt.serialize();
  }
}
