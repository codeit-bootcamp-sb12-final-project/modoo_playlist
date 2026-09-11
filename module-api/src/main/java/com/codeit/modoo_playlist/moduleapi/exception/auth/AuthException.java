package com.codeit.modoo_playlist.moduleapi.exception.auth;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

public class AuthException extends BaseException {

  public AuthException(ErrorCode errorCode) {
    super(errorCode);
  }
}
