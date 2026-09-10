package com.codeit.modoo_playlist.moduleapi.exception.user;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

public class EmailAlreadyExistsException extends UserException {

  public EmailAlreadyExistsException() {
    super(ErrorCode.EMAIL_ALREADY_EXISTS);
  }

  public static EmailAlreadyExistsException withEmail(String email) {
    EmailAlreadyExistsException ex = new EmailAlreadyExistsException();
    ex.addDetail("email", email);
    return ex;
  }
}
