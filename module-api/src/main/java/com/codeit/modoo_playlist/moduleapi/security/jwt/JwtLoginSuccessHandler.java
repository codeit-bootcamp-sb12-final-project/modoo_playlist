package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.AuthService;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.JwtDto;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginIssueResult;
import com.codeit.modoo_playlist.moduleapi.security.SecurityErrorResponseWriter;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
  private final AuthService authService;
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
      LoginIssueResult result = authService.issueLogin(
          userDetails.getUserDto().id()
      );

      refreshCookie = tokenProvider.generateRefreshTokenCookie(
          result.refreshToken(),
          result.expiresAt()
      );

      JwtDto jwtDto = new JwtDto(
          result.userDto(),
          result.accessToken()
      );

      responseBody = objectMapper.writeValueAsString(jwtDto);
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