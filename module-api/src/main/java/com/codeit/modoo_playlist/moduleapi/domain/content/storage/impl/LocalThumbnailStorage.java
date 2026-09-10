package com.codeit.modoo_playlist.moduleapi.domain.content.storage.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

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
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_WIDTH = 4096;
    private static final int MAX_HEIGHT = 4096;
    private static final long MAX_PIXELS = 16_000_000L;

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
        if (thumbnail.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("썸네일 파일은 5MB 이하여야 합니다.");
        }

        String contentType = thumbnail.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 썸네일로 등록할 수 있습니다.");
        }

        String extension = resolveExtension(thumbnail.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("jpg, jpeg, png, gif 형식만 등록할 수 있습니다.");
        }

        validateImageContent(thumbnail, extension);

        return extension;
    }

    private void validateImageContent(MultipartFile thumbnail, String extension) {
        try (var inputStream = thumbnail.getInputStream();
             ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            if (imageInputStream == null) {
                throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;

                if (width <= 0 || height <= 0
                        || width > MAX_WIDTH || height > MAX_HEIGHT
                        || pixels > MAX_PIXELS) {
                    throw new IllegalArgumentException("썸네일 해상도가 허용 범위를 초과했습니다.");
                }
                if (!matchesExtension(extension, reader.getFormatName())) {
                    throw new IllegalArgumentException("파일 확장자와 실제 이미지 형식이 일치하지 않습니다.");
                }
                if (reader.read(0) == null) {
                    throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.", exception);
        }
    }

    private boolean matchesExtension(String extension, String formatName) {
        String normalizedFormat = formatName.toLowerCase(Locale.ROOT);
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return "jpg".equals(normalizedFormat) || "jpeg".equals(normalizedFormat);
        }
        return extension.equals(normalizedFormat);
    }

    private String resolveExtension(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null) {
            return "";
        }
        return extension.toLowerCase(Locale.ROOT);
    }
}
