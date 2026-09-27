package com.codeit.modoo_playlist.core.global.common.dto.base;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SliceCursorRequestTest {

  @Test
  void cursor가_없으면_형식_검증을_통과한다() {
    SliceCursorRequest request = new SliceCursorRequest(null, null, 10, null, null);

    assertThat(request.isCursorFormatValid()).isTrue();
  }

  @Test
  void cursor가_Instant_형식이면_형식_검증을_통과한다() {
    SliceCursorRequest request =
        new SliceCursorRequest(Instant.now().toString(), UUID.randomUUID(), 10, null, null);

    assertThat(request.isCursorFormatValid()).isTrue();
  }

  @Test
  void cursor가_Instant_형식이_아니면_형식_검증에_실패한다() {
    SliceCursorRequest request =
        new SliceCursorRequest("유효하지-않은-커서", UUID.randomUUID(), 10, null, null);

    assertThat(request.isCursorFormatValid()).isFalse();
  }
}
