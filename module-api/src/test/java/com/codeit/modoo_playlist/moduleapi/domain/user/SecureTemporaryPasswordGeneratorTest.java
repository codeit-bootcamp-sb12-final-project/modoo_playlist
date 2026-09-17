package com.codeit.modoo_playlist.moduleapi.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.SecureTemporaryPasswordGenerator;
import org.junit.jupiter.api.Test;

class SecureTemporaryPasswordGeneratorTest {

  @Test
  void generatesConfiguredLength() {
    SecureTemporaryPasswordGenerator generator =
        new SecureTemporaryPasswordGenerator(10);

    assertThat(generator.generate()).hasSize(10);
  }
}
