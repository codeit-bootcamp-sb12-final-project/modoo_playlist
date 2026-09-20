package com.codeit.modoo_playlist.moduleapi.domain.preference.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.codeit.modoo_playlist.core.domain.preference.entity.UserPreferenceTag;
import com.codeit.modoo_playlist.core.domain.preference.entity.UserPreferenceTagId;
import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = UserPreferenceTagRepositoryTest.JpaConfig.class)
class UserPreferenceTagRepositoryTest {

  @Configuration(proxyBeanMethods = false)
  @EntityScan(basePackages = "com.codeit.modoo_playlist.core.domain")
  @EnableJpaRepositories(basePackageClasses = UserPreferenceTagRepository.class)
  @EnableJpaAuditing
  static class JpaConfig {

  }

  @Autowired private UserPreferenceTagRepository userPreferenceTagRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void 사용자의_취향_태그를_점수_내림차순으로_조회한다() {
    User user = persistUser("user@example.com");
    Tag action = persistTag("액션", TagKind.GENRE);
    Tag drama = persistTag("드라마", TagKind.GENRE);
    persistPreference(user, action, "3.0000");
    persistPreference(user, drama, "5.0000");
    flushAndClear();

    List<UserPreferenceTagDto> result =
        userPreferenceTagRepository.findTopTagsByUserId(user.getId(), PageRequest.of(0, 10));

    assertThat(result).extracting(UserPreferenceTagDto::tagName).containsExactly("드라마", "액션");
    assertThat(result).extracting(UserPreferenceTagDto::tagId)
        .containsExactly(drama.getId(), action.getId());
    assertThat(result).extracting(UserPreferenceTagDto::tagKind).containsOnly(TagKind.GENRE);
    assertThat(result.get(0).score()).isEqualByComparingTo("5.0000");
  }

  @Test
  void limit만큼만_조회하고_다른_사용자의_태그는_섞이지_않는다() {
    User user = persistUser("user@example.com");
    User other = persistUser("other@example.com");
    Tag action = persistTag("액션", TagKind.GENRE);
    Tag drama = persistTag("드라마", TagKind.GENRE);
    Tag comedy = persistTag("코미디", TagKind.GENRE);
    persistPreference(user, action, "1.0000");
    persistPreference(user, drama, "2.0000");
    persistPreference(other, comedy, "9.0000");
    flushAndClear();

    List<UserPreferenceTagDto> result =
        userPreferenceTagRepository.findTopTagsByUserId(user.getId(), PageRequest.of(0, 1));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).tagName()).isEqualTo("드라마");
  }

  private User persistUser(String email) {
    User user = User.create(email, email, "encoded-password");
    entityManager.persist(user);
    return user;
  }

  private Tag persistTag(String name, TagKind kind) {
    Tag tag = Tag.builder().name(name).kind(kind).build();
    entityManager.persist(tag);
    return tag;
  }

  private void persistPreference(User user, Tag tag, String score) {
    entityManager.persist(UserPreferenceTag.builder()
        .id(new UserPreferenceTagId(user.getId(), tag.getId()))
        .user(user).tag(tag)
        .score(new BigDecimal(score))
        .rawScore(new BigDecimal(score))
        .build());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
