package com.codeit.modoo_playlist.modulerealtime;

import com.codeit.modoo_playlist.infra.config.JpaConfig;
import com.codeit.modoo_playlist.infra.config.QuerydslConfig;
import com.codeit.modoo_playlist.infra.event.kafka.RealtimeKafkaPublisher;
import com.codeit.modoo_playlist.infra.security.AccessTokenVerifier;
import com.codeit.modoo_playlist.infra.store.RedisLoginSessionStore;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
		"com.codeit.modoo_playlist.modulerealtime",
		"com.codeit.modoo_playlist.infra.mapper"
})
@EntityScan("com.codeit.modoo_playlist.core")
@EnableJpaRepositories("com.codeit.modoo_playlist.infra")
@EnableScheduling
@Import({
		AccessTokenVerifier.class,
		RedisLoginSessionStore.class,
		JsonMapper.class,
		JpaConfig.class,
		RealtimeKafkaPublisher.class,
		QuerydslConfig.class
})
public class ModuleRealtimeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ModuleRealtimeApplication.class, args);
	}

}
