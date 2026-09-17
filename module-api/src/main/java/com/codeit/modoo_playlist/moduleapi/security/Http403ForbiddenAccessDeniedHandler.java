package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class Http403ForbiddenAccessDeniedHandler implements AccessDeniedHandler {

  private final HandlerExceptionResolver exceptionResolver;
  private final SecurityErrorResponseWriter errorResponseWriter;

  public Http403ForbiddenAccessDeniedHandler(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
      SecurityErrorResponseWriter errorResponseWriter
  ) {
    this.exceptionResolver = exceptionResolver;
    this.errorResponseWriter = errorResponseWriter;
  }

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

    BaseException baseException = new BaseException(errorCode, exception);
    if (exceptionResolver.resolveException(request, response, null, baseException) == null) {
      errorResponseWriter.write(response, baseException);
    }
  }
}
