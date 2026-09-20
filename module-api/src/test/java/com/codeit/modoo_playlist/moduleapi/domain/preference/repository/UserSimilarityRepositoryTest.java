package com.codeit.modoo_playlist.moduleapi.domain.preference.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.codeit.modoo_playlist.core.domain.preference.entity.UserSimilarity;
import com.codeit.modoo_playlist.core.domain.preference.entity.UserSimilarityId;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.SimilarUserDto;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = UserSimilarityRepositoryTest.JpaConfig.class)
class UserSimilarityRepositoryTest {

  @Configuration(proxyBeanMethods = false)
  @EntityScan(basePackages = "com.codeit.modoo_playlist.core.domain")
  @EnableJpaRepositories(basePackageClasses = UserSimilarityRepository.class)
  static class JpaConfig {

  }

  @Autowired private UserSimilarityRepository userSimilarityRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void 유사_사용자를_점수_내림차순으로_조회하고_필드를_매핑한다() {
    User me = persistUser("me@example.com", "me");
    User closer = persistUser("closer@example.com", "closer");
    User farther = persistUser("farther@example.com", "farther");
    closer.updateProfile("closer", "closer.png");
    persistSimilarity(me, closer, "0.9000", "액션,드라마");
    persistSimilarity(me, farther, "0.3000", "코미디");
    flushAndClear();

    List<SimilarUserDto> result =
        userSimilarityRepository.findTopSimilarUsersByUserId(me.getId(), PageRequest.of(0, 10));

    assertThat(result).extracting(SimilarUserDto::username).containsExactly("closer", "farther");
    SimilarUserDto top = result.get(0);
    assertThat(top.userId()).isEqualTo(closer.getId());
    assertThat(top.profileImageUrl()).isEqualTo("closer.png");
    assertThat(top.score()).isEqualByComparingTo("0.9000");
    assertThat(top.sharedTags()).isEqualTo("액션,드라마");
  }

  @Test
  void limit만큼만_조회하고_다른_사용자_기준_유사도는_섞이지_않는다() {
    User me = persistUser("me@example.com", "me");
    User other = persistUser("other@example.com", "other");
    User a = persistUser("a@example.com", "a");
    User b = persistUser("b@example.com", "b");
    persistSimilarity(me, a, "0.5000", "액션");
    persistSimilarity(me, b, "0.7000", "드라마");
    persistSimilarity(other, a, "0.9999", "코미디");
    flushAndClear();

    List<SimilarUserDto> result =
        userSimilarityRepository.findTopSimilarUsersByUserId(me.getId(), PageRequest.of(0, 1));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).username()).isEqualTo("b");
  }

  private User persistUser(String email, String username) {
    User user = User.create(email, username, "encoded-password");
    entityManager.persist(user);
    return user;
  }

  private void persistSimilarity(User user, User otherUser, String score, String sharedTags) {
    entityManager.persist(UserSimilarity.builder()
        .id(new UserSimilarityId(user.getId(), otherUser.getId()))
        .user(user).otherUser(otherUser)
        .score(new BigDecimal(score))
        .sharedTags(sharedTags)
        .computedAt(Instant.now())
        .build());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
