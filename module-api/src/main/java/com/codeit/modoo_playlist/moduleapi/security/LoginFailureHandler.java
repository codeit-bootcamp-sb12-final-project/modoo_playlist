package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.core.global.exception.ErrorResponse;
import com.codeit.modoo_playlist.moduleapi.exception.user.UserNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

  private final JsonMapper objectMapper;

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {
    boolean badCredentials = exception instanceof BadCredentialsException
        || exception instanceof UsernameNotFoundException
        || (exception instanceof InternalAuthenticationServiceException
        && exception.getCause() instanceof UserNotFoundException);
    if (badCredentials) {
      log.debug("Login rejected: invalid credentials");
    } else {
      log.error("Authentication failed", exception);
    }
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    // 이메일 존재 여부 및 프레임워크의 로케일과 무관하게 같은 응답을 반환한다.
    AuthenticationException responseException = badCredentials
        ? new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.")
        : exception;
    ErrorResponse errorResponse = new ErrorResponse(responseException,
        HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
