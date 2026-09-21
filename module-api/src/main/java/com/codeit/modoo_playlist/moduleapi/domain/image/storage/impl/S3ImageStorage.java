package com.codeit.modoo_playlist.moduleapi.domain.image.storage.impl;

import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageCategory;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageStorage;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageValidator;
import com.codeit.modoo_playlist.infra.config.StorageProperties;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3ImageStorage implements ImageStorage {
  private final S3Client s3Client;
  private final ImageValidator imageValidator;
  private final StorageProperties storageProperties;

  @Override
  public String store(MultipartFile image, ImageCategory category) throws IOException {
    String extension = imageValidator.validate(image);
    String key = category.directory() + "/" + UUID.randomUUID() + "." + extension;
    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(storageProperties.getS3().getBucket())
        .key(key)
        .contentType(contentType(extension))
        .build();
    try (var inputStream = image.getInputStream()) {
      s3Client.putObject(request, RequestBody.fromInputStream(inputStream, image.getSize()));
    } catch (SdkException exception) {
      throw new IOException("S3 이미지 저장에 실패했습니다.", exception);
    }
    return normalizedBaseUrl() + "/" + key;
  }

  @Override
  public void delete(String imageUrl) throws IOException {
    String prefix = normalizedBaseUrl() + "/";
    if (imageUrl == null || !imageUrl.startsWith(prefix)) return;
    String key = imageUrl.substring(prefix.length());
    if (key.isBlank() || key.contains("..")) return;
    try {
      s3Client.deleteObject(DeleteObjectRequest.builder()
          .bucket(storageProperties.getS3().getBucket()).key(key).build());
    } catch (SdkException exception) {
      throw new IOException("S3 이미지 삭제에 실패했습니다.", exception);
    }
  }

  private String normalizedBaseUrl() {
    String publicBaseUrl = storageProperties.getS3().getPublicBaseUrl();
    return publicBaseUrl.endsWith("/")
        ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
        : publicBaseUrl;
  }

  private String contentType(String extension) {
    return "png".equals(extension) ? "image/png" : "image/jpeg";
  }
}
