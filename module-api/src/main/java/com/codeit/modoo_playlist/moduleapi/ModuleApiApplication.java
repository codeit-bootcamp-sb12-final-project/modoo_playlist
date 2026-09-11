package com.codeit.modoo_playlist.moduleapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = {
    "com.codeit.modoo_playlist.moduleapi",
    "com.codeit.modoo_playlist.infra"
})
@EntityScan(basePackages = {
    "com.codeit.modoo_playlist.core",
})
@EnableJpaRepositories(basePackages = {
    "com.codeit.modoo_playlist.core",
    "com.codeit.modoo_playlist.moduleapi"
})
public class ModuleApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModuleApiApplication.class, args);
  }

}
