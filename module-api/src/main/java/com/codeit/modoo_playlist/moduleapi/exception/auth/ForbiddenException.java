package com.codeit.modoo_playlist.moduleapi.exception.auth;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

public class ForbiddenException extends AuthException {

  public ForbiddenException() {
    super(ErrorCode.ACCESS_DENIED);
  }

}
