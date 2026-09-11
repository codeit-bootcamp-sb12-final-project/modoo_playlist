package com.codeit.modoo_playlist.moduleapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "module-api.auth.cookie")
public record AuthCookieProperties(
    boolean secure,
    String sameSite,
    String path
) {

}
