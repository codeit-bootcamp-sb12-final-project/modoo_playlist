package com.codeit.modoo_playlist.moduleapi.domain.recommendation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentPerson;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentPersonRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = ContentPersonRepositoryBatchQueryTest.JpaConfig.class)
class ContentPersonRepositoryBatchQueryTest {

  @Configuration(proxyBeanMethods = false)
  @EntityScan(basePackages = "com.codeit.modoo_playlist.core.domain")
  @EnableJpaRepositories(
      basePackageClasses = ContentPersonRepository.class,
      includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ContentPersonRepository.class))
  @EnableJpaAuditing
  static class JpaConfig {

  }

  @Autowired private ContentPersonRepository contentPersonRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void 요청한_콘텐츠들의_인물만_한_번에_조회하고_표시순서대로_반환한다() {
    Content first = persistContent("영화1");
    Content second = persistContent("영화2");
    Content notRequested = persistContent("영화3");
    persistPerson(first, "ACTOR", "배우1-2", 2);
    persistPerson(first, "DIRECTOR", "감독1", 0);
    persistPerson(first, "ACTOR", "배우1-1", 1);
    persistPerson(second, "ACTOR", "배우2-1", 1);
    persistPerson(notRequested, "ACTOR", "제외대상", 0);
    flushAndClear();

    List<ContentPerson> people = contentPersonRepository
        .findAllByContent_IdInOrderByDisplayOrderAscIdAsc(List.of(first.getId(), second.getId()));

    assertThat(people).extracting(ContentPerson::getPersonName)
        .doesNotContain("제외대상")
        .containsExactlyInAnyOrder("감독1", "배우1-1", "배우1-2", "배우2-1");
    assertThat(people.stream().filter(person -> person.getContent().getId().equals(first.getId())))
        .extracting(ContentPerson::getPersonName)
        .containsExactly("감독1", "배우1-1", "배우1-2");
  }

  @Test
  void 표시순서가_같으면_id순으로_반환한다() {
    Content content = persistContent("영화");
    persistPerson(content, uuid(3), "ACTOR", "배우3", 1);
    persistPerson(content, uuid(1), "ACTOR", "배우1", 1);
    persistPerson(content, uuid(2), "ACTOR", "배우2", 1);
    flushAndClear();

    List<ContentPerson> people = contentPersonRepository
        .findAllByContent_IdInOrderByDisplayOrderAscIdAsc(List.of(content.getId()));

    assertThat(people).extracting(ContentPerson::getPersonName).containsExactly("배우1", "배우2", "배우3");
  }

  @Test
  void 조회한_인물의_콘텐츠_ID_접근은_콘텐츠를_추가로_조회하지_않는다() {
    Content content = persistContent("영화");
    persistPerson(content, "ACTOR", "배우", 0);
    flushAndClear();

    List<ContentPerson> people = contentPersonRepository
        .findAllByContent_IdInOrderByDisplayOrderAscIdAsc(List.of(content.getId()));
    UUID contentId = people.get(0).getContent().getId();

    assertThat(contentId).isEqualTo(content.getId());
    assertThat(Hibernate.isInitialized(people.get(0).getContent())).isFalse();
  }

  @Test
  void 인물이_없거나_없는_콘텐츠를_요청하면_빈_결과를_반환한다() {
    Content content = persistContent("영화");
    flushAndClear();

    assertThat(contentPersonRepository.findAllByContent_IdInOrderByDisplayOrderAscIdAsc(List.of(content.getId())))
        .isEmpty();
    assertThat(contentPersonRepository.findAllByContent_IdInOrderByDisplayOrderAscIdAsc(List.of(UUID.randomUUID())))
        .isEmpty();
  }

  private UUID uuid(int n) {
    return UUID.fromString("00000000-0000-7000-8000-00000000000" + n);
  }

  private Content persistContent(String title) {
    Content content = Content.builder().type(ContentType.MOVIE).title(title).build();
    entityManager.persist(content);
    return content;
  }

  private void persistPerson(Content content, String roleType, String name, int displayOrder) {
    entityManager.persist(ContentPerson.builder()
        .content(content).roleType(roleType).personName(name).displayOrder(displayOrder).build());
  }

  private void persistPerson(Content content, UUID id, String roleType, String name, int displayOrder) {
    entityManager.persist(ContentPerson.builder()
        .id(id).content(content).roleType(roleType).personName(name).displayOrder(displayOrder).build());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
