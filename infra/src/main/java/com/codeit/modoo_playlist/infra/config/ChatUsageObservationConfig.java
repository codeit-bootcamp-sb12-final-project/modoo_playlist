package com.codeit.modoo_playlist.infra.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatUsageObservationConfig {

  @Bean
  public ObservationRegistry observationRegistry() {
    ObservationRegistry registry = ObservationRegistry.create();
    registry.observationConfig()
        .observationPredicate((name, context) -> name.startsWith("spring.ai.") || name.startsWith("gen_ai."))
        .observationHandler(new ChatUsageObservationHandler());
    return registry;
  }
}
