package com.codeit.modoo_playlist.moduleapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.config.properties.BotProperties;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BotInitializerTest {

  private static final String EMAIL = "bot@example.com";
  private static final String NAME = "modoo-bot";

  @Mock
  UserRepository userRepository;

  @Test
  @DisplayName("BOT==0. BOT 생성")
  void createsBotWhenMissing() {
    when(userRepository.findAllByRole(UserRole.BOT)).thenReturn(List.of());
    when(userRepository.existsByEmail(EMAIL)).thenReturn(false);

    initializer(new BotProperties(true, EMAIL, NAME)).run(null);

    verify(userRepository).save(assertArg(bot -> {
      assertThat(bot.getEmail()).isEqualTo(EMAIL);
      assertThat(bot.getUsername()).isEqualTo(NAME);
      assertThat(bot.getRole()).isEqualTo(UserRole.BOT);
      assertThat(bot.getPassword()).isNull();
      assertThat(bot.isLocked()).isFalse();
    }));
  }

  @Test
  @DisplayName("BOT있으면 동기화.")
  void synchronizesExistingBot() {
    User bot = User.createBot("old-bot@example.com", "old-name");
    ReflectionTestUtils.setField(bot, "id", UUID.randomUUID());
    bot.setPassword("legacy-password");
    bot.changeLocked(true);

    when(userRepository.findAllByRole(UserRole.BOT)).thenReturn(List.of(bot));
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    initializer(new BotProperties(true, EMAIL, NAME)).run(null);

    assertThat(bot.getEmail()).isEqualTo(EMAIL);
    assertThat(bot.getUsername()).isEqualTo(NAME);
    assertThat(bot.getPassword()).isNull();
    assertThat(bot.isLocked()).isFalse();

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("BOT>2 초기화 실패")
  void rejectsMultipleBots() {
    when(userRepository.findAllByRole(UserRole.BOT))
        .thenReturn(List.of(
            User.createBot("bot1@example.com", "bot1"),
            User.createBot("bot2@example.com", "bot2")));

    assertError(
        initializer(new BotProperties(true, EMAIL, NAME)),
        ErrorCode.MULTIPLE_BOT_ACCOUNTS);
  }

  @Test
  @DisplayName("BOT 필수 설정이 없으면 초기화 실패.")
  void rejectsInvalidConfiguration() {
    when(userRepository.findAllByRole(UserRole.BOT)).thenReturn(List.of());

    assertError(
        initializer(new BotProperties(true, " ", NAME)),
        ErrorCode.BOT_CONFIGURATION_INVALID);
  }

  @Test
  @DisplayName("새 BOT 이메일을 일반 사용자가 쓰고 있으면 초기화를 중단.")
  void rejectsEmailConflict() {
    when(userRepository.findAllByRole(UserRole.BOT)).thenReturn(List.of());
    when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

    assertError(
        initializer(new BotProperties(true, EMAIL, NAME)),
        ErrorCode.BOT_EMAIL_CONFLICT);
    
    verify(userRepository, never()).save(any());
  }

  private BotInitializer initializer(BotProperties properties) {
    return new BotInitializer(properties, userRepository);
  }

  private void assertError(BotInitializer initializer, ErrorCode errorCode) {
    assertThatThrownBy(() -> initializer.run(null))
        .isInstanceOfSatisfying(BaseException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
  }
}
