package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.core.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

// 공통 응답기.
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

  private final JsonMapper objectMapper;

  public void write(
      HttpServletResponse response,
      ErrorCode errorCode
  ) throws IOException {
    write(response, new BaseException(errorCode));
  }

  public void write(
      HttpServletResponse response,
      BaseException exception
  ) throws IOException {
    ErrorCode errorCode = exception.getErrorCode();

    response.setStatus(errorCode.getStatus());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    ErrorResponse errorResponse = new ErrorResponse(
        exception,
        errorCode.getStatus()
    );

    response.getWriter().write(
        objectMapper.writeValueAsString(errorResponse)
    );
  }
}