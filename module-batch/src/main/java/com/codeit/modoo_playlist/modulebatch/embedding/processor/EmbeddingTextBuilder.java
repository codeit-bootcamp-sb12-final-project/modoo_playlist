package com.codeit.modoo_playlist.modulebatch.embedding.processor;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EmbeddingTextBuilder {

  private static final String STORE_GENERATION = "es-v1";
  private static final Set<String> ISO_COUNTRY_CODES = Set.of(Locale.getISOCountries());

  private EmbeddingTextBuilder() {
  }

  // Gemini 비대칭 검색 문서 포맷. 쿼리 쪽은 module-api의 SearchQueryTextBuilder가 짝을 맞춤
  public static String build(ContentEmbeddingTarget target) {
    String title = target.title() == null ? "none" : target.title();
    String body = Stream.of(
            segment("유형", typeLabel(target.type())),
            segment("연도", target.releaseYear() == null ? null : target.releaseYear() + "년"),
            segment("국가", countryLabel(target.originCountry())),
            segment("태그", spaced(target.tagNames())),
            segment("감독", spaced(target.directors())),
            segment("출연", spaced(target.actors())),
            segment("종목", target.sportType()),
            segment("리그", target.league()),
            segment("시즌", target.season()),
            segment("경기", matchLabel(target.homeTeam(), target.awayTeam())),
            segment("장소", target.venue()),
            segment("줄거리", target.description()))
        .filter(Objects::nonNull)
        .collect(Collectors.joining(" | "));
    return "title: " + title + " | text: " + body;
  }

  private static String segment(String label, String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return label + ": " + value.trim();
  }

  private static String typeLabel(String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "MOVIE" -> "영화";
      case "TV" -> "TV 시리즈";
      case "SPORT" -> "스포츠";
      default -> type;
    };
  }

  private static String countryLabel(String country) {
    if (country == null) {
      return null;
    }
    String code = country.trim().toUpperCase(Locale.ROOT);
    if (!ISO_COUNTRY_CODES.contains(code)) {
      return country;
    }
    return new Locale("", code).getDisplayCountry(Locale.KOREAN) + " (" + code + ")";
  }

  private static String matchLabel(String homeTeam, String awayTeam) {
    boolean hasHome = homeTeam != null && !homeTeam.isBlank();
    boolean hasAway = awayTeam != null && !awayTeam.isBlank();
    if (hasHome && hasAway) {
      return homeTeam.trim() + " vs " + awayTeam.trim();
    }
    if (hasHome) {
      return homeTeam;
    }
    return hasAway ? awayTeam : null;
  }

  private static String spaced(String commaSeparated) {
    return commaSeparated == null ? null : commaSeparated.replace(",", ", ");
  }

  public static String hash(String text, String model, String thumbnailUrl) {
    String normalizedThumbnail = thumbnailUrl == null ? "" : thumbnailUrl;
    return sha256(model + "::" + STORE_GENERATION + "::" + normalizedThumbnail + "::" + text);
  }

  private static String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
