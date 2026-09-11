package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.config.AuthCookieProperties;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";
  private static final Duration ISSUED_AT_CLOCK_SKEW = Duration.ofSeconds(30);

  // 만료시간 조회 (RedisJwtRegistry의 TTL 설정용)
  @Getter
  private final long accessTokenExpirationMs;

  @Getter
  private final long refreshTokenExpirationMs;
  private final String issuer;

  private final JWSSigner accessTokenSigner;
  private final JWSVerifier accessTokenVerifier;
  private final JWSSigner refreshTokenSigner;
  private final JWSVerifier refreshTokenVerifier;

  //  쿠키 설정
  private final AuthCookieProperties cookieProperties;

  public JwtTokenProvider(
      @Value("${module-api.jwt.access-token.secret}") String accessTokenSecret,
      @Value("${module-api.jwt.access-token.expiration-ms}") long accessTokenExpirationMs,
      @Value("${module-api.jwt.refresh-token.secret}") String refreshTokenSecret,
      @Value("${module-api.jwt.refresh-token.expiration-ms}") long refreshTokenExpirationMs,
      @Value("${module-api.jwt.issuer}") String issuer,
      AuthCookieProperties cookieProperties
  ) throws JOSEException {
    this.issuer = issuer;
    this.accessTokenExpirationMs = accessTokenExpirationMs;
    this.refreshTokenExpirationMs = refreshTokenExpirationMs;

    byte[] accessKey = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
    byte[] refreshKey = refreshTokenSecret.getBytes(StandardCharsets.UTF_8);

    this.accessTokenSigner = new MACSigner(accessKey);
    this.accessTokenVerifier = new MACVerifier(accessKey);

    this.refreshTokenSigner = new MACSigner(refreshKey);
    this.refreshTokenVerifier = new MACVerifier(refreshKey);

    this.cookieProperties = cookieProperties;
  }

  public String generateAccessToken(
      UserDetails userDetails,
      UUID sid,
      Instant sessionExpiresAt
  ) throws JOSEException {
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant sessionExpiry = sessionExpiresAt.truncatedTo(ChronoUnit.SECONDS);

    if (!sessionExpiry.isAfter(now)) {
      throw new IllegalArgumentException("로그인 세션이 만료되었습니다.");
    }

    Instant accessExpiry = now.plusMillis(accessTokenExpirationMs)
        .truncatedTo(ChronoUnit.SECONDS);

    Instant expiresAt = accessExpiry.isBefore(sessionExpiry)
        ? accessExpiry
        : sessionExpiry;

    return generateToken(
        userDetails,
        sid,
        now,
        expiresAt,
        accessTokenSigner,
        "access"
    );
  }

  public String generateRefreshToken(
      UserDetails userDetails,
      UUID sid,
      Instant sessionExpiresAt
  ) throws JOSEException {
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant expiresAt = sessionExpiresAt.truncatedTo(ChronoUnit.SECONDS);

    if (!expiresAt.isAfter(now)) {
      throw new IllegalArgumentException("로그인 세션이 만료되었습니다.");
    }

    return generateToken(
        userDetails,
        sid,
        now,
        expiresAt,
        refreshTokenSigner,
        "refresh"
    );
  }

  private String generateToken(
      UserDetails userDetails,
      UUID sid,
      Instant issuedAt,
      Instant expiresAt,
      JWSSigner signer,
      String tokenType
  ) throws JOSEException {
    UserDto user = userDetails.getUserDto();

    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(user.email())
        .claim("userId", user.id().toString())
        .claim("sid", sid.toString())
        .claim("type", tokenType)
        .jwtID(UUID.randomUUID().toString())
        .issueTime(Date.from(issuedAt))
        .expirationTime(Date.from(expiresAt))
        .build();

    SignedJWT signedJWT = new SignedJWT(
        new JWSHeader(JWSAlgorithm.HS256),
        claims
    );

    signedJWT.sign(signer);

    log.debug("Generated {} token for user: {}", tokenType, user.name());
    return signedJWT.serialize();
  }

  public boolean validateAccessToken(String token) {
    return validateToken(token, accessTokenVerifier, "access");
  }

  public boolean validateRefreshToken(String token) {
    return validateToken(token, refreshTokenVerifier, "refresh");
  }

  private boolean validateToken(
      String token,
      JWSVerifier verifier,
      String expectedType
  ) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);

      if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())) {
        return false;
      }

      if (!signedJWT.verify(verifier)) {
        log.debug("JWT signature verification failed for {} token", expectedType);
        return false;
      }

      JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

      if (!issuer.equals(claims.getIssuer())) {
        return false;
      }

      String tokenType = claims.getStringClaim("type");
      if (!expectedType.equals(tokenType)) {
        log.debug("JWT token type mismatch: expected {}, got {}", expectedType, tokenType);
        return false;
      }

      Instant now = Instant.now();
      Date expiration = claims.getExpirationTime();
      Date issuedAt = claims.getIssueTime();

//      now 포함을 위해 not 사용
      if (expiration == null || !expiration.toInstant().isAfter(now)) {
        log.debug("JWT {} token expired", expectedType);
        return false;
      }

      if (issuedAt == null
          || issuedAt.toInstant().isAfter(now.plus(ISSUED_AT_CLOCK_SKEW))
          || !issuedAt.before(expiration)) {
        return false;
      }

      if (claims.getSubject() == null || claims.getSubject().isBlank()
          || claims.getJWTID() == null || claims.getJWTID().isBlank()) {
        return false;
      }

      String userId = claims.getStringClaim("userId");
      String sid = claims.getStringClaim("sid");

      if (userId == null || sid == null) {
        return false;
      }

//      마지막 UUID 형식 검사
      UUID.fromString(userId);
      UUID.fromString(sid);

      return true;
    } catch (Exception e) {
      log.debug("JWT validation failed for {} token", expectedType);
      return false;
    }
  }

  public String getUsernameFromToken(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      return signedJWT.getJWTClaimsSet().getSubject();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JWT token", e);
    }
  }

  public UUID getUserId(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      String userIdStr = (String) signedJWT.getJWTClaimsSet().getClaim("userId");
      if (userIdStr == null) {
        throw new IllegalArgumentException("User ID claim not found in JWT token");
      }
      return UUID.fromString(userIdStr);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JWT token", e);
    }
  }

  public UUID getSid(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);

      String sid = signedJWT.getJWTClaimsSet()
          .getStringClaim("sid");

      if (sid == null) {
        throw new IllegalArgumentException("SID claim not found in JWT token");
      }

      return UUID.fromString(sid);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JWT token", e);
    }
  }

  // JWT 문자열 생성을 하고, 쿠키 객체를 만든 다음
  // 로그인 성공 핸들러에서 응답으로 브라우저에 쿠키 저장하도록.
  public Cookie generateRefreshTokenCookie(
      String refreshToken,
      Instant sessionExpiresAt
  ) {
//    시간이 1초 언더인 경우
    long remainingSeconds = Duration.between(
        Instant.now(),
        sessionExpiresAt
    ).getSeconds();
    if (remainingSeconds <= 0) {
      throw new BaseException(ErrorCode.INVALID_TOKEN);
    }
    Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);

    applyRefreshCookiePolicy(refreshCookie);
    refreshCookie.setMaxAge((int) remainingSeconds);
    return refreshCookie;
  }

  public Cookie generateRefreshTokenExpirationCookie() {
    Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");

    applyRefreshCookiePolicy(refreshCookie);
    refreshCookie.setMaxAge(0);

    return refreshCookie;
  }

  private void applyRefreshCookiePolicy(Cookie cookie) {
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieProperties.secure());
    cookie.setPath(cookieProperties.path());
    cookie.setAttribute("SameSite", cookieProperties.sameSite());
  }
}
