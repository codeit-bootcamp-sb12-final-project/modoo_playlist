package com.codeit.modoo_playlist.moduleapi.domain.content.storage.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.codeit.modoo_playlist.infra.config.FileConfig;
import com.codeit.modoo_playlist.moduleapi.domain.content.storage.ThumbnailStorage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LocalThumbnailStorage implements ThumbnailStorage {

    private static final String THUMBNAIL_DIRECTORY = "thumbnails";
    private static final String THUMBNAIL_URL_PREFIX = "/files/thumbnails/";

    private final FileConfig fileConfig;

    @Override
    public String store(MultipartFile thumbnail) throws IOException {
        validateImage(thumbnail);

        Path thumbnailDirectory = fileConfig.getRootPath().resolve(THUMBNAIL_DIRECTORY);
        Files.createDirectories(thumbnailDirectory);

        String storedFilename = UUID.randomUUID() + resolveExtension(thumbnail.getOriginalFilename());
        Path destination = thumbnailDirectory.resolve(storedFilename);

        try (var inputStream = thumbnail.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        return THUMBNAIL_URL_PREFIX + storedFilename;
    }

    private void validateImage(MultipartFile thumbnail) {
        if (thumbnail == null || thumbnail.isEmpty()) {
            throw new IllegalArgumentException("썸네일 파일이 비어 있습니다.");
        }

        String contentType = thumbnail.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 썸네일로 등록할 수 있습니다.");
        }
    }

    private String resolveExtension(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null || !extension.matches("[A-Za-z0-9]{1,10}")) {
            return "";
        }
        return "." + extension.toLowerCase(Locale.ROOT);
    }
}
