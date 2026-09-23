package com.codeit.modoo_playlist.moduleapi.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "module-api.cors")
public record CorsProperties(
    List<String> allowedOrigins
) {

  public CorsProperties {
    allowedOrigins = allowedOrigins == null
        ? List.of()
        : allowedOrigins.stream()
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .distinct()
            .toList();

    if (allowedOrigins.contains("*")) {
      throw new IllegalArgumentException(
          "Wildcard CORS origins cannot be used when credentials are allowed"
      );
    }
  }
}
