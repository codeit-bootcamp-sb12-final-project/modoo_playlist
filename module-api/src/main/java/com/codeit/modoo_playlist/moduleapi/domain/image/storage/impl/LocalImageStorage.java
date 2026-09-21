package com.codeit.modoo_playlist.moduleapi.domain.image.storage.impl;

import com.codeit.modoo_playlist.infra.config.FileConfig;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageCategory;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageStorage;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {
  private static final String URL_PREFIX = "/files/";

  private final FileConfig fileConfig;
  private final ImageValidator imageValidator;

  @Override
  public String store(MultipartFile image, ImageCategory category) throws IOException {
    String extension = imageValidator.validate(image);
    Path directory = fileConfig.getRootPath().resolve(category.directory());
    Files.createDirectories(directory);
    String filename = UUID.randomUUID() + "." + extension;
    try (var inputStream = image.getInputStream()) {
      Files.copy(inputStream, directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
    }
    return URL_PREFIX + category.directory() + "/" + filename;
  }

  @Override
  public void delete(String imageUrl) throws IOException {
    if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) return;
    String relativePath = imageUrl.substring(URL_PREFIX.length());
    Path root = fileConfig.getRootPath().normalize();
    Path target = root.resolve(relativePath).normalize();
    if (target.startsWith(root) && !target.equals(root)) Files.deleteIfExists(target);
  }
}
