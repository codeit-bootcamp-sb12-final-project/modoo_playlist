package com.codeit.modoo_playlist.moduleapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "module-api.admin")
public record AdminProperties(
    boolean enabled,
    String email,
    String password,
    String name
) {

}