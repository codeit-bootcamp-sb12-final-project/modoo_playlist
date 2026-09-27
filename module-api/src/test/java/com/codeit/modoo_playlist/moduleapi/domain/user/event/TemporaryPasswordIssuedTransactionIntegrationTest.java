package com.codeit.modoo_playlist.moduleapi.domain.user.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.infra.config.QuerydslConfig;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.AuthService;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.TemporaryPasswordGenerator;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.TemporaryPasswordSender;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.AuthServiceImpl;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import com.codeit.modoo_playlist.moduleapi.security.UserDetailsService;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import com.codeit.modoo_playlist.core.global.security.LoginSessionStore;
import com.codeit.modoo_playlist.moduleapi.security.jwt.RefreshTokenHasher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = TemporaryPasswordIssuedTransactionIntegrationTest.JpaConfig.class
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TemporaryPasswordIssuedTransactionIntegrationTest {

  private static final String EMAIL = "user@example.com";
  private static final String TEMPORARY_PASSWORD = "temporary1!!";
  private static final String ENCODED_TEMPORARY_PASSWORD = "encoded-temporary-password";
  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  @Autowired
  AuthService authService;

  @Autowired
  UserRepository userRepository;

  @MockitoBean
  JwtTokenProvider tokenProvider;

  @MockitoBean
  LoginSessionStore loginSessionStore;

  @MockitoBean
  UserDetailsService userDetailsService;

  @MockitoBean
  RefreshTokenHasher refreshTokenHasher;

  @MockitoBean
  PasswordEncoder passwordEncoder;

  @MockitoBean
  TemporaryPasswordGenerator temporaryPasswordGenerator;

  @MockitoBean
  TemporaryPasswordSender temporaryPasswordSender;

  @MockitoBean
  UserMapper userMapper;

  @Test
  void runsPasswordResetSideEffectsAfterServiceTransactionCommits() {
    User user = userRepository.saveAndFlush(
        User.create(EMAIL, "user", "encoded-password")
    );
    UUID userId = user.getId();

    when(temporaryPasswordGenerator.generate()).thenReturn(TEMPORARY_PASSWORD);
    when(passwordEncoder.encode(TEMPORARY_PASSWORD))
        .thenReturn(ENCODED_TEMPORARY_PASSWORD);

    authService.resetPassword(EMAIL);

    User stored = userRepository.findById(userId).orElseThrow();
    assertThat(stored.getTempPassword()).isEqualTo(ENCODED_TEMPORARY_PASSWORD);
    assertThat(stored.getTempPasswordExpiresAt()).isEqualTo(NOW.plusSeconds(180));

    InOrder order = inOrder(loginSessionStore, temporaryPasswordSender);
    order.verify(loginSessionStore).invalidateAll(userId);
    order.verify(temporaryPasswordSender)
        .send(EMAIL, TEMPORARY_PASSWORD, NOW.plusSeconds(180));
  }

  @Configuration(proxyBeanMethods = false)
  @EntityScan(basePackageClasses = User.class)
  @EnableJpaRepositories(basePackageClasses = UserRepository.class)
  @EnableJpaAuditing
  @Import({
      QuerydslConfig.class,
      AuthServiceImpl.class,
      TemporaryPasswordIssuedEventListener.class
  })
  static class JpaConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
