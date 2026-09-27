package com.codeit.modoo_playlist.moduleapi.config;

import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.infra.event.kafka.RealtimeKafkaPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("!prod & !docker")
public class RealtimeNotifierConfig {

    @Bean
    public RealtimeNotifier apiRealtimeNotifier(
            KafkaTemplate<String, Object> kafkaTemplate) {
        return new RealtimeKafkaPublisher(kafkaTemplate);
    }
}