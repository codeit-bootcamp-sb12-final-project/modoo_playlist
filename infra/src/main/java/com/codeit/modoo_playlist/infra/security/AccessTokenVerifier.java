package com.codeit.modoo_playlist.infra.security;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.core.global.security.AccessTokenClaims;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

// access토큰 검증기: JwtTokenProvider에서 access 토큰을 검증하는 기능만 존재.
@Component
public class AccessTokenVerifier {

    private static final Duration ISSUED_AT_CLOCK_SKEW =
            Duration.ofSeconds(30);

    private final JWSVerifier verifier;
    private final String issuer;

    public AccessTokenVerifier(
            @Value("${module-api.jwt.access-token.secret}") String secret,
            @Value("${module-api.jwt.issuer}") String issuer
    ) throws JOSEException {
        this.verifier = new MACVerifier(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.issuer = issuer;
    }

    public AccessTokenClaims verify(String token) {
        if (token == null || token.isBlank()) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }

        try {
            SignedJWT jwt = SignedJWT.parse(token);

            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(verifier)) {
                throw new BaseException(ErrorCode.INVALID_TOKEN);
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            if (!issuer.equals(claims.getIssuer())
                    || !"access".equals(claims.getStringClaim("type"))) {
                throw new BaseException(ErrorCode.INVALID_TOKEN);
            }

            Instant now = Instant.now();
            Date expiration = claims.getExpirationTime();
            Date issuedAt = claims.getIssueTime();

            if (expiration == null || !expiration.toInstant().isAfter(now)) {
                throw new BaseException(ErrorCode.INVALID_TOKEN);
            }

            if (issuedAt == null
                    || issuedAt.toInstant().isAfter(now.plus(ISSUED_AT_CLOCK_SKEW))
                    || !issuedAt.before(expiration)) {
                throw new BaseException(ErrorCode.INVALID_TOKEN);
            }

            String email = claims.getSubject();
            String jwtId = claims.getJWTID();
            String userId = claims.getStringClaim("userId");
            String sid = claims.getStringClaim("sid");

            if(email == null || email.isBlank()
            || jwtId == null || jwtId.isBlank()
            || userId == null || sid == null) {
                throw new BaseException(ErrorCode.INVALID_TOKEN);
            }

            return new AccessTokenClaims(
                    UUID.fromString(userId),
                    UUID.fromString(sid),
                    email
            );

        } catch (ParseException | JOSEException | IllegalArgumentException e) {
            throw new BaseException(ErrorCode.INVALID_TOKEN,e);
        }
    }
}