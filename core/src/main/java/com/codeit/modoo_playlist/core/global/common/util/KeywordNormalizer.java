package com.codeit.modoo_playlist.core.global.common.util;

import java.text.Normalizer;
import java.util.Locale;

public class KeywordNormalizer {

  private static final int MAX_LENGTH = 50;

  private KeywordNormalizer() {

  }

  // 검색어 정규화
  public static String normalize(String rawKeyword) {
    if (rawKeyword == null) {
      return "";
    }
    return Normalizer.normalize(rawKeyword, Normalizer.Form.NFKC)
        .strip()
        .replaceAll("\\s+", " ")
        .toLowerCase(Locale.ROOT);
  }

  public static boolean isValidLength(String normalized) {
    if (normalized == null || normalized.isEmpty()) {
      return false;
    }
    return codePointLength(normalized) <= MAX_LENGTH;
  }

  public static int codePointLength(String normalized) {
    if (normalized == null) {
      return 0;
    }
    return normalized.codePointCount(0, normalized.length());
  }

}
