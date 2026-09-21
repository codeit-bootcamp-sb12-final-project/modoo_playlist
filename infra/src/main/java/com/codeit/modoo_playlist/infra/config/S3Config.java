package com.codeit.modoo_playlist.infra.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
@EnableConfigurationProperties(StorageProperties.class)
public class S3Config {

    private final StorageProperties storageProperties;

    public S3Config(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    // S3 업로드/삭제용
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(storageProperties.getS3().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
