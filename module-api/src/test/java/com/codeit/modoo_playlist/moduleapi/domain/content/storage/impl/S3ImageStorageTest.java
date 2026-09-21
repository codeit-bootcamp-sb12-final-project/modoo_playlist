package com.codeit.modoo_playlist.moduleapi.domain.content.storage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageCategory;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageValidator;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.impl.S3ImageStorage;
import com.codeit.modoo_playlist.infra.config.StorageProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3ImageStorageTest {

  @Test
  void S3_설정값이_비어있으면_시작전_검증에서_거부한다() {
    StorageProperties properties = new StorageProperties();
    properties.setType(StorageProperties.StorageType.S3);

    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(properties)).isNotEmpty();
    }
  }

  @Test
  void CloudFront_URL을_반환하고_같은_key로_삭제한다() throws Exception {
    S3Client s3Client = mock(S3Client.class);
    StorageProperties properties = new StorageProperties();
    properties.getS3().setBucket("image-bucket");
    properties.getS3().setRegion("ap-northeast-2");
    properties.getS3().setPublicBaseUrl("https://cdn.example.com/");
    S3ImageStorage storage = new S3ImageStorage(s3Client, new ImageValidator(), properties);
    MockMultipartFile image = new MockMultipartFile(
        "image", "profile.png", "image/png", pngBytes()
    );

    String url = storage.store(image, ImageCategory.USER_PROFILE);

    assertThat(url).matches("https://cdn\\.example\\.com/users/profiles/[0-9a-f-]+\\.png");
    ArgumentCaptor<PutObjectRequest> putRequest = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(putRequest.capture(), any(RequestBody.class));
    assertThat(putRequest.getValue().bucket()).isEqualTo("image-bucket");
    assertThat(putRequest.getValue().key()).startsWith("users/profiles/");
    assertThat(putRequest.getValue().contentType()).isEqualTo("image/png");

    storage.delete(url);

    ArgumentCaptor<DeleteObjectRequest> deleteRequest =
        ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client).deleteObject(deleteRequest.capture());
    assertThat(deleteRequest.getValue().key()).isEqualTo(putRequest.getValue().key());
  }

  private byte[] pngBytes() throws Exception {
    BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }
}
