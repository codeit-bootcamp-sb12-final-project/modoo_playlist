package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.JwtDto;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginSession;
import com.codeit.modoo_playlist.moduleapi.security.SecurityErrorResponseWriter;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JsonMapper objectMapper;
  private final JwtTokenProvider tokenProvider;
  private final LoginSessionStore loginSessionStore;
  private final RefreshTokenHasher refreshTokenHasher;
  private final SecurityErrorResponseWriter errorResponseWriter;

  /*
    인증 성공 순서

    현재 프로젝트의 인증 객체가 맞는지 체크
    -> sid, 생성 시간, 만료시간, 액세스 토큰, 갱신 토큰 생성.
    -> LoginSession 생성.
    -> 브라우저에 전달할 객체 생성(갱신 토큰과 만료시간, JwtDto)
    -> redis에 loginsession 저장.
    -> 브라우저에 정보 전달.
   */
  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException {

    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader("Cache-Control", "no-store");

//    다른 인증 객체 오면 에러.
    if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
      SecurityContextHolder.clearContext();
      log.error("Unexpected principal type in login success handler");

      errorResponseWriter.write(
          response,
          ErrorCode.INTERNAL_SERVER_ERROR
      );
      return;
    }

    Cookie refreshCookie;
    String responseBody;

    try {
      Instant createdAt = Instant.now()
          .truncatedTo(ChronoUnit.SECONDS);

      UUID sid = UUID.randomUUID();

      Instant expiresAt = createdAt
          .plusMillis(tokenProvider.getRefreshTokenExpirationMs())
          .truncatedTo(ChronoUnit.SECONDS);

      String accessToken = tokenProvider.generateAccessToken(
          userDetails, sid, expiresAt
      );

      String refreshToken = tokenProvider.generateRefreshToken(
          userDetails, sid, expiresAt
      );

      LoginSession session = new LoginSession(
          sid,
          userDetails.getUserDto().id(),
          refreshTokenHasher.hash(refreshToken),
          createdAt,
          expiresAt
      );

//      브라우저에 전달할 쿠키와 jwtdto.
      refreshCookie = tokenProvider.generateRefreshTokenCookie(
          refreshToken, expiresAt
      );

      JwtDto jwtDto = new JwtDto(
          userDetails.getUserDto(),
          accessToken
      );

      responseBody = objectMapper.writeValueAsString(jwtDto);

      loginSessionStore.register(session);

    } catch (BaseException e) {
      SecurityContextHolder.clearContext();
      log.error("Login processing failed: {}", e.getErrorCode(), e);

      errorResponseWriter.write(response, e);
      return;

    } catch (DataAccessException e) {
      SecurityContextHolder.clearContext();
      log.error("Failed to store login session", e);

      errorResponseWriter.write(
          response,
          ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE
      );
      return;

    } catch (JOSEException e) {
      SecurityContextHolder.clearContext();
      log.error("Failed to generate login tokens", e);

      errorResponseWriter.write(
          response,
          ErrorCode.TOKEN_GENERATION_FAILED
      );
      return;

    } catch (Exception e) {
      SecurityContextHolder.clearContext();
      log.error("Failed to complete login", e);

      errorResponseWriter.write(
          response,
          ErrorCode.INTERNAL_SERVER_ERROR
      );
      return;
    }

    // 응답 전송 중 발생한 IOException은 위의 준비·저장 실패와 구분한다.
    response.setStatus(HttpServletResponse.SC_OK);
    response.addCookie(refreshCookie);
    response.getWriter().write(responseBody);
  }
}