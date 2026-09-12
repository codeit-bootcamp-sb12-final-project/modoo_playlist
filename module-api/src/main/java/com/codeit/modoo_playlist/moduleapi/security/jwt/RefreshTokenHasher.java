package com.codeit.modoo_playlist.moduleapi.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenHasher {

  public String hash(String refreshToken) {
    try {
      byte[] hash = MessageDigest
          .getInstance("SHA-256")
          .digest(
              refreshToken.getBytes(
                  StandardCharsets.UTF_8
              )
          );

      return HexFormat.of().formatHex(hash);

    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
