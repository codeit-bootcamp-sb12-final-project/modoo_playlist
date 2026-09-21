package com.codeit.modoo_playlist.moduleapi.domain.content.storage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageCategory;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageValidator;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.impl.LocalImageStorage;

class LocalThumbnailStorageTest {

    @TempDir Path tempDirectory;

    @Test
    void 실제_PNG_이미지를_UUID_파일명으로_저장한다() throws Exception {
        LocalImageStorage storage = new LocalImageStorage(() -> tempDirectory, new ImageValidator());
        MockMultipartFile image = new MockMultipartFile(
                "thumbnail", "poster.PNG", "image/png", pngBytes()
        );

        String url = storage.store(image, ImageCategory.CONTENT_THUMBNAIL);

        assertThat(url).matches("/files/contents/thumbnails/[0-9a-f-]+\\.png");
        assertThat(Files.exists(tempDirectory.resolve(url.substring("/files/".length())))).isTrue();
    }

    @Test
    void ContentType이_없으면_거부한다() throws Exception {
        assertInvalid(new MockMultipartFile("thumbnail", "poster.png", null, pngBytes()),
                ErrorCode.THUMBNAIL_INVALID);
    }

    @Test
    void 허용하지_않는_확장자는_거부한다() throws Exception {
        assertInvalid(new MockMultipartFile("thumbnail", "poster.svg", "image/svg+xml", pngBytes()),
                ErrorCode.THUMBNAIL_INVALID);
    }

    @Test
    void 확장자와_실제_이미지_형식이_다르면_거부한다() throws Exception {
        assertInvalid(new MockMultipartFile("thumbnail", "poster.jpg", "image/jpeg", pngBytes()),
                ErrorCode.THUMBNAIL_INVALID);
    }

    @Test
    void 이미지로_위장한_파일은_거부한다() {
        assertInvalid(new MockMultipartFile(
                "thumbnail", "poster.png", "image/png", "not-image".getBytes()
        ), ErrorCode.THUMBNAIL_INVALID);
    }

    @Test
    void 파일크기가_5MB를_초과하면_디코딩_전에_거부한다() {
        assertInvalid(new MockMultipartFile(
                "thumbnail", "poster.png", "image/png", new byte[5 * 1024 * 1024 + 1]
        ), ErrorCode.PAYLOAD_TOO_LARGE);
    }

    private void assertInvalid(MockMultipartFile file, ErrorCode errorCode) {
        LocalImageStorage storage = new LocalImageStorage(() -> tempDirectory, new ImageValidator());
        assertThatThrownBy(() -> storage.store(file, ImageCategory.CONTENT_THUMBNAIL))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
