package com.codeit.modoo_playlist.moduleapi.security.jwt;

//import com.codeit.modoo_playlist.infra.security.BlogUserDetails;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
    import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final String issuer;

    private final JWSSigner accessTokenSigner;
    private final JWSVerifier accessTokenVerifier;
    private final JWSSigner refreshTokenSigner;
    private final JWSVerifier refreshTokenVerifier;

    public JwtTokenProvider(
        @Value("${module-api.jwt.access-token.secret}") String accessTokenSecret,
        @Value("${module-api.jwt.access-token.expiration-ms}") int accessTokenExpirationMs,
        @Value("${module-api.jwt.refresh-token.secret}") String refreshTokenSecret,
        @Value("${module-api.jwt.refresh-token.expiration-ms}") int refreshTokenExpirationMs,
        @Value("${module-api.jwt.issuer}") String issuer
    ) throws JOSEException {
        this.issuer = issuer;
        this.accessTokenExpirationMs = accessTokenExpirationMs * 1000L;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs * 1000L;

        byte[] secretBytes = accessTokenSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSigner = new MACSigner(secretBytes);
        this.accessTokenVerifier = new MACVerifier(secretBytes);
        this.refreshTokenSigner = new MACSigner(secretBytes);
        this.refreshTokenVerifier = new MACVerifier(secretBytes);
    }

//    public String generateAccessToken(BlogUserDetails userDetails) throws JOSEException {
//        return generateToken(userDetails, accessTokenExpirationMs, accessTokenSigner, "access");
//    }
//
//    public String generateRefreshToken(BlogUserDetails userDetails) throws JOSEException {
//        return generateToken(userDetails, refreshTokenExpirationMs, refreshTokenSigner, "refresh");
//    }
//
//    private String generateToken(BlogUserDetails userDetails, long expirationMs, JWSSigner signer,
//            String tokenType) throws JOSEException {
//        String tokenId = UUID.randomUUID().toString();
//        UserDto user = userDetails.getUserDto();
//
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + expirationMs);
//
//        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
//                .subject(user.username())
//                .jwtID(tokenId)
//                .issuer(issuer)
//                .claim("userId", user.id().toString())
//                .claim("type", tokenType)
//                .claim("nickname", user.nickname())
//                .claim("email", user.email())
//                .claim("roles", userDetails.getAuthorities().stream()
//                        .map(GrantedAuthority::getAuthority)
//                        .collect(Collectors.toList()))
//                .issueTime(now)
//                .expirationTime(expiryDate)
//                .build();
//
//        SignedJWT signedJWT = new SignedJWT(
//                new JWSHeader(JWSAlgorithm.HS256),
//                claimsSet
//        );
//
//        signedJWT.sign(signer);
//        String token = signedJWT.serialize();
//
//        log.debug("Generated {} token for user: {}", tokenType, user.username());
//        return token;
//    }

    // 만료시간 조회 (RedisJwtRegistry의 TTL 설정용)
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, accessTokenVerifier, "access");
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshTokenVerifier, "refresh");
    }

    private boolean validateToken(String token, JWSVerifier verifier, String expectedType) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                log.debug("JWT signature verification failed for {} token", expectedType);
                return false;
            }

            String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
            if (!expectedType.equals(tokenType)) {
                log.debug("JWT token type mismatch: expected {}, got {}", expectedType, tokenType);
                return false;
            }

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                log.debug("JWT {} token expired", expectedType);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.debug("JWT {} token validation failed: {}", expectedType, e.getMessage());
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

    public Cookie genereateRefreshTokenCookie(String refreshToken) {
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true); // Use HTTPS in production
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) (refreshTokenExpirationMs / 1000));
        return refreshCookie;
    }

    public Cookie genereateRefreshTokenExpirationCookie() {
        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true); // Use HTTPS in production
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        return refreshCookie;
    }
}
