package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.moduleapi.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<User> users = userRepository.findAll();

        int updatedCount = 0;

        for (User user : users) {
            String rawPassword = user.getPassword();

            // 이미 BCrypt로 인코딩된 경우 skip
            if (rawPassword != null &&
                    (rawPassword.startsWith("$2a$") || rawPassword.startsWith("$2b$") || rawPassword.startsWith("$2y$"))) {
                continue;
            }

            String encoded = passwordEncoder.encode(rawPassword);
            user.setPassword(encoded);
            updatedCount++;
        }

        if (updatedCount > 0) {
            userRepository.saveAll(users);
            log.info("UserInitializer: {}명의 사용자 비밀번호를 인코딩했습니다.", updatedCount);
        } else {
            log.info("UserInitializer: 인코딩할 비밀번호가 없습니다.");
        }
    }
}
