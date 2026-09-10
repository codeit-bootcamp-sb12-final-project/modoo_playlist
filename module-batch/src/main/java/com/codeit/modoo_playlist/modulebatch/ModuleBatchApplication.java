package com.codeit.modoo_playlist.modulebatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.codeit.modoo_playlist.infra.client.tmdb.TmdbClientConfig;

@SpringBootApplication
@EnableScheduling
@Import(TmdbClientConfig.class)
public class ModuleBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(ModuleBatchApplication.class, args);
	}

}
