package com.codeit.modoo_playlist.moduleapi.exception.user;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

public class UserException extends BaseException {

  public UserException(ErrorCode errorCode) {
    super(errorCode);
  }
}
