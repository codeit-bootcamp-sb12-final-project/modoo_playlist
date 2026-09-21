package com.codeit.modoo_playlist.moduleapi.domain.image.storage;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageValidator {
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
  private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
  private static final int MAX_WIDTH = 4096;
  private static final int MAX_HEIGHT = 4096;
  private static final long MAX_PIXELS = 16_000_000L;

  public String validate(MultipartFile image) {
    if (image == null || image.isEmpty()) throw invalidImage("empty");
    if (image.getSize() > MAX_FILE_SIZE) throw new BaseException(ErrorCode.PAYLOAD_TOO_LARGE);
    String contentType = image.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) throw invalidImage("contentType");
    String extension = resolveExtension(image.getOriginalFilename());
    if (!ALLOWED_EXTENSIONS.contains(extension)) throw invalidImage("extension");
    validateContent(image, extension);
    return extension;
  }

  private void validateContent(MultipartFile image, String extension) {
    try (var inputStream = image.getInputStream();
        ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
      if (imageInputStream == null) throw invalidImage("content");
      Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
      if (!readers.hasNext()) throw invalidImage("content");
      ImageReader reader = readers.next();
      try {
        reader.setInput(imageInputStream, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        long pixels = (long) width * height;
        if (width <= 0 || height <= 0 || width > MAX_WIDTH || height > MAX_HEIGHT
            || pixels > MAX_PIXELS) throw invalidImage("dimensions");
        if (!matchesExtension(extension, reader.getFormatName())) throw invalidImage("formatMismatch");
        if (reader.read(0) == null) throw invalidImage("content");
      } finally {
        reader.dispose();
      }
    } catch (IOException exception) {
      throw new BaseException(ErrorCode.THUMBNAIL_INVALID, exception);
    }
  }

  private boolean matchesExtension(String extension, String formatName) {
    String normalized = formatName.toLowerCase(Locale.ROOT);
    return ("jpg".equals(extension) || "jpeg".equals(extension))
        ? "jpg".equals(normalized) || "jpeg".equals(normalized)
        : extension.equals(normalized);
  }

  private String resolveExtension(String filename) {
    String extension = StringUtils.getFilenameExtension(filename);
    return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
  }

  private BaseException invalidImage(String reason) {
    BaseException exception = new BaseException(ErrorCode.THUMBNAIL_INVALID);
    exception.addDetail("reason", reason);
    return exception;
  }
}
