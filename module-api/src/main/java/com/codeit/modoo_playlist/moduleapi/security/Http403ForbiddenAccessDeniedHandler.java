package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Http403ForbiddenAccessDeniedHandler implements AccessDeniedHandler {

  private final SecurityErrorResponseWriter errorResponseWriter;

  //  ErrorCode 별로 분리해서 코드 전달.
  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException exception
  ) throws IOException {
    ErrorCode errorCode = exception instanceof CsrfException
        ? ErrorCode.INVALID_CSRF_TOKEN
        : ErrorCode.ACCESS_DENIED;

    errorResponseWriter.write(response, errorCode);
  }
}
