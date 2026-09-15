package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

  private final SecurityErrorResponseWriter errorResponseWriter;

  //  이메일 에러 부분은 create나 find 쪽에서 컨트롤 하니까 여기에서는 제외.
  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception
  ) throws IOException {
    ErrorCode errorCode;

    if (exception instanceof LockedException) {
      errorCode = ErrorCode.USER_ACCOUNT_LOCKED;
    } else if (isInvalidCredentials(exception)) {
      errorCode = ErrorCode.INVALID_CREDENTIALS;
    } else if (
        exception instanceof InternalAuthenticationServiceException
            && exception.getCause() instanceof DataAccessException
    ) {
      errorCode = ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE;
    } else {
      errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    }

    errorResponseWriter.write(response, errorCode);
  }

  private boolean isInvalidCredentials(AuthenticationException exception) {
    return exception instanceof BadCredentialsException
        || exception instanceof UsernameNotFoundException;
  }
}
