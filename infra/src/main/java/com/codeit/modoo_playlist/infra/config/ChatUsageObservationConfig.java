package com.codeit.modoo_playlist.infra.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatUsageObservationConfig {

  @Bean
  public ObservationRegistry observationRegistry() {
    ObservationRegistry registry = ObservationRegistry.create();
    registry.observationConfig().observationHandler(new ChatUsageObservationHandler());
    return registry;
  }
}
