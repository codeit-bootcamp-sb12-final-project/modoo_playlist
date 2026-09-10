package com.codeit.modoo_playlist.moduleapi.domain.content.storage.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

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
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");

    private final FileConfig fileConfig;

    @Override
    public String store(MultipartFile thumbnail) throws IOException {
        String extension = validateImage(thumbnail);

        Path thumbnailDirectory = fileConfig.getRootPath().resolve(THUMBNAIL_DIRECTORY);
        Files.createDirectories(thumbnailDirectory);

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path destination = thumbnailDirectory.resolve(storedFilename);

        try (var inputStream = thumbnail.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        return THUMBNAIL_URL_PREFIX + storedFilename;
    }

    private String validateImage(MultipartFile thumbnail) throws IOException {
        if (thumbnail == null || thumbnail.isEmpty()) {
            throw new IllegalArgumentException("썸네일 파일이 비어 있습니다.");
        }

        String contentType = thumbnail.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 썸네일로 등록할 수 있습니다.");
        }

        String extension = resolveExtension(thumbnail.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("jpg, jpeg, png, gif 형식만 등록할 수 있습니다.");
        }

        try (var inputStream = thumbnail.getInputStream()) {
            if (ImageIO.read(inputStream) == null) {
                throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");
            }
        }

        return extension;
    }

    private String resolveExtension(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null) {
            return "";
        }
        return extension.toLowerCase(Locale.ROOT);
    }
}
