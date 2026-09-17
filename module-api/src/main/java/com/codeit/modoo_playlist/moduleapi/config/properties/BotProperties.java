package com.codeit.modoo_playlist.moduleapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "module-api.bot")
public record BotProperties(
    boolean enabled,
    String email,
    String name
) {

}