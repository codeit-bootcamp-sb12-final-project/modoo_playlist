package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

@Getter
public class CodedAuthenticationException extends AuthenticationException {

  private final ErrorCode errorCode;

  public CodedAuthenticationException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
