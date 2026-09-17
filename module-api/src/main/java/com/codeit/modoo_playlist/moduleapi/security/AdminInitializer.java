package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.config.properties.AdminProperties;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

  private final AdminProperties properties;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!properties.enabled()) {
      return;
    }

    if (userRepository.existsByRole(UserRole.ADMIN)) {
      return;
    }

    validateProperties();

//    admin 이메일이 이미 존재한다면 해당 이메일을 승격하는 것이 아니라 실패 처리로
    userRepository.findByEmail(properties.email())
        .ifPresent(user -> {
          throw new BaseException(ErrorCode.ADMIN_EMAIL_CONFLICT);
        });

    User admin = User.create(
        properties.email(),
        properties.name(),
        passwordEncoder.encode(properties.password())
    );

    admin.changeRole(UserRole.ADMIN);
    userRepository.save(admin);
  }

  private void validateProperties() {
    if (!StringUtils.hasText(properties.email())
        || !StringUtils.hasText(properties.password())) {
      throw new BaseException(ErrorCode.ADMIN_CONFIGURATION_INVALID);
    }
  }
}