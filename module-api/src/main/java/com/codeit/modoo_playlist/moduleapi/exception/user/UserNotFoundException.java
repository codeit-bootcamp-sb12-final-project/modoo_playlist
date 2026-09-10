package com.codeit.modoo_playlist.moduleapi.exception.user;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import java.util.UUID;

public class UserNotFoundException extends UserException {

  public UserNotFoundException() {
    super(ErrorCode.USER_NOT_FOUND);
  }

  public static UserNotFoundException withUsername(String username) {
    UserNotFoundException ex = new UserNotFoundException();
    ex.addDetail("username", username);
    return ex;
  }

  public static UserNotFoundException withUserId(UUID userId) {
    UserNotFoundException ex = new UserNotFoundException();
    ex.addDetail("userId", userId);
    return ex;
  }

  public static UserNotFoundException withUserEmail(String email) {
    UserNotFoundException ex = new UserNotFoundException();
    ex.addDetail("email", email);
    return ex;
  }
}
