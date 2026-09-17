package com.codeit.modoo_playlist.moduleapi.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = UserRepositoryTest.JpaConfig.class)
class UserRepositoryTest {

  private static final String EMAIL = "user@example.com";
  private static final String MISSING_EMAIL = "missing@example.com";
  private static final String USERNAME = "original";
  private static final String PASSWORD = "encoded";
  private static final String CHANGED_USERNAME = "changed";
  private static final String ORIGINAL_IMAGE = "original.png";
  private static final String CHANGED_IMAGE = "new.png";

  @Configuration(proxyBeanMethods = false)
  @EntityScan(basePackageClasses = User.class)
  @EnableJpaRepositories(basePackageClasses = UserRepository.class)
  @EnableJpaAuditing
  static class JpaConfig {

  }

  @Autowired
  UserRepository users;
  @Autowired
  EntityManager entityManager;
  private User user;

  @BeforeEach
  void setUp() {
    user = User.create(EMAIL, USERNAME, PASSWORD);
  }

  @Test
  @DisplayName("저장 후 clear 및 존재 체크.")
  void saveAndReload() {
    users.saveAndFlush(user);
    UUID id = user.getId();
    entityManager.clear();

    User loaded = users.findById(id).orElseThrow();
    assertThat(loaded.getEmail()).isEqualTo(EMAIL);
    assertThat(loaded.getUsername()).isEqualTo(USERNAME);
    assertThat(loaded.getPassword()).isEqualTo(PASSWORD);
    assertThat(loaded.getRole()).isEqualTo(UserRole.USER);
    assertThat(loaded.isLocked()).isFalse();
    assertThat(loaded.getCreatedAt()).isNotNull();
    assertThat(loaded.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("이메일로 사용자 조회 및 존재 체크")
  void findByEmail() {
    users.saveAndFlush(user);
    entityManager.clear();

    assertThat(users.findByEmail(EMAIL)).get()
        .extracting(User::getId).isEqualTo(user.getId());
    assertThat(users.existsByEmail(EMAIL)).isTrue();
    assertThat(users.findByEmail(MISSING_EMAIL)).isEmpty();
    assertThat(users.existsByEmail(MISSING_EMAIL)).isFalse();
  }

  @Test
  @DisplayName("중복 이메일은 실제 DB UNIQUE 제약에 의해 차단된다")
  void uniqueEmailConstraint() {
    users.saveAndFlush(user);

    assertThatThrownBy(
        () -> users.saveAndFlush(User.create(EMAIL, "duplicate", PASSWORD)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("프로필 변경은 dirty checking으로 저장.")
  void updateProfilePersists() {
    users.saveAndFlush(user);
    user.updateProfile(CHANGED_USERNAME, CHANGED_IMAGE);
    entityManager.flush();
    entityManager.clear();

    User loaded = users.findById(user.getId()).orElseThrow();
    assertThat(loaded.getUsername()).isEqualTo(CHANGED_USERNAME);
    assertThat(loaded.getProfileImageUrl()).isEqualTo(CHANGED_IMAGE);
    assertThat(loaded.getEmail()).isEqualTo(EMAIL);
    assertThat(loaded.getPassword()).isEqualTo(PASSWORD);
    assertThat(loaded.getRole()).isEqualTo(UserRole.USER);
  }

  @Test
  @DisplayName("이미지 없이 이름만 변경하면 기존 이미지가 DB에 유지")
  void retainImage() {
    user.updateProfile(USERNAME, ORIGINAL_IMAGE);
    users.saveAndFlush(user);
    user.updateProfile(CHANGED_USERNAME, null);
    entityManager.flush();
    entityManager.clear();

    assertThat(users.findById(user.getId()).orElseThrow().getProfileImageUrl()).isEqualTo(
        ORIGINAL_IMAGE);
  }
}
