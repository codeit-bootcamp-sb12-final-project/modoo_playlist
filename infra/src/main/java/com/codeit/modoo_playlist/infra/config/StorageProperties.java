package com.codeit.modoo_playlist.infra.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    @NotNull
    private StorageType type = StorageType.LOCAL;

    @Valid
    private S3 s3 = new S3();

    @AssertTrue(message = "S3 저장소 사용 시 bucket과 public-base-url은 필수입니다.")
    public boolean isS3ConfigurationValid() {
        if (type != StorageType.S3) {
            return true;
        }
        return hasText(s3.bucket) && hasText(s3.region) && hasText(s3.publicBaseUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public enum StorageType {
        LOCAL,
        S3
    }

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
        private String region;
        private String publicBaseUrl;
    }
}
