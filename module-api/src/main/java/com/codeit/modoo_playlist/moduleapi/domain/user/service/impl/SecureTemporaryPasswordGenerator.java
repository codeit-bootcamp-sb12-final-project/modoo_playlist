package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.moduleapi.domain.user.service.TemporaryPasswordGenerator;
import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecureTemporaryPasswordGenerator implements TemporaryPasswordGenerator {

  private static final char[] CHARACTERS =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();

  private final SecureRandom secureRandom = new SecureRandom();
  private final int length;

  public SecureTemporaryPasswordGenerator(
      @Value("${module-api.auth.temporary-password.length:8}") int length
  ) {
    if (length < 8) {
      throw new IllegalArgumentException("임시 비밀번호 길이는 8자 이상이어야 합니다.");
    }
    this.length = length;
  }

  @Override
  public String generate() {
    StringBuilder password = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
      password.append(CHARACTERS[secureRandom.nextInt(CHARACTERS.length)]);
    }

    return password.toString();
  }
}
