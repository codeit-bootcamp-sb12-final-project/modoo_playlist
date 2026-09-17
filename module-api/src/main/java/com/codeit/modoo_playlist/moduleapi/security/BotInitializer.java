package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.config.properties.BotProperties;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class BotInitializer implements ApplicationRunner {

  private final BotProperties properties;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!properties.enabled()) {
      return;
    }

    List<User> bots =
        userRepository.findAllByRole(UserRole.BOT);

    if (bots.size() > 1) {
      throw new BaseException(ErrorCode.MULTIPLE_BOT_ACCOUNTS);
    }

    validateProperties();

    if (bots.size() == 1) {
      synchronizeExistingBot(bots.get(0));
      return;
    }

    validateEmailAvailableForNewBot();

    userRepository.save(
        User.createBot(
            properties.email(),
            properties.name()
        )
    );
  }

  private void synchronizeExistingBot(User bot) {
    userRepository.findByEmail(properties.email())
        .filter(existing ->
            !existing.getId().equals(bot.getId())
        )
        .ifPresent(existing -> {
          throw new BaseException(ErrorCode.BOT_EMAIL_CONFLICT);
        });

    if (Objects.equals(bot.getEmail(), properties.email())
        && Objects.equals(bot.getUsername(), properties.name())
        && !bot.isLocked()
        && bot.getPassword() == null) {
      return;
    }

    bot.synchronizeBot(
        properties.email(),
        properties.name()
    );
  }

  private void validateProperties() {
    if (!StringUtils.hasText(properties.email())
        || !StringUtils.hasText(properties.name())) {
      throw new BaseException(ErrorCode.BOT_CONFIGURATION_INVALID);
    }
  }

  //  bot이 0일때만 실행 됨.
  private void validateEmailAvailableForNewBot() {
    if (userRepository.existsByEmail(properties.email())) {
      throw new BaseException(ErrorCode.BOT_EMAIL_CONFLICT);
    }
  }
}