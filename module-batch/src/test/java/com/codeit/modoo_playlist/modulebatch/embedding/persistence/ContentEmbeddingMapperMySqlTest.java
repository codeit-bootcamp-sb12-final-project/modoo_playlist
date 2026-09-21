package com.codeit.modoo_playlist.modulebatch.embedding.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;

@MybatisTest(properties = {
        "spring.datasource.url=jdbc:tc:mysql:8.4.7:///modoo_mysql",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../infra/src/main/resources/schema.sql",
        "spring.test.database.replace=NONE",
        "mybatis.mapper-locations=classpath*:mapper/**/*.xml"
})
@ContextConfiguration(classes = ContentEmbeddingMapperMySqlTest.MyBatisTestConfiguration.class)
class ContentEmbeddingMapperMySqlTest {

  @Autowired private ContentEmbeddingMapper mapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @MapperScan("com.codeit.modoo_playlist.modulebatch.embedding.persistence")
  static class MyBatisTestConfiguration {
  }

  @Test
  void 태그이름을_알파벳순으로_합쳐서_반환한다() {
    String contentId = insertContent("영화A", false, null);
    attachTag(contentId, "Drama");
    attachTag(contentId, "Action");

    List<ContentEmbeddingTarget> targets = mapper.findContentsNeedingEmbedding(0, 10);

    assertThat(targets).filteredOn(t -> t.contentId().equals(contentId))
        .extracting(ContentEmbeddingTarget::tagNames)
        .containsExactly("Action,Drama");
  }

  @Test
  void 태그가_없는_콘텐츠도_후보에_포함되고_tagNames는_null이다() {
    String contentId = insertContent("영화B", false, null);

    List<ContentEmbeddingTarget> targets = mapper.findContentsNeedingEmbedding(0, 10);

    assertThat(targets).filteredOn(t -> t.contentId().equals(contentId))
        .extracting(ContentEmbeddingTarget::tagNames)
        .containsExactly((String) null);
  }

  @Test
  void 삭제된_콘텐츠는_후보에서_제외된다() {
    String contentId = insertContent("영화C", true, null);

    List<ContentEmbeddingTarget> targets = mapper.findContentsNeedingEmbedding(0, 10);

    assertThat(targets).extracting(ContentEmbeddingTarget::contentId).doesNotContain(contentId);
  }

  @Test
  void updateEmbeddingSourceHash는_해시를_갱신한다() {
    String contentId = insertContent("영화D", false, null);

    mapper.updateEmbeddingSourceHash(contentId, "new-hash");

    assertThat(jdbcTemplate.queryForObject(
        "SELECT embedding_source_hash FROM contents WHERE id = UUID_TO_BIN(?)",
        String.class, contentId)).isEqualTo("new-hash");
  }

  @Test
  void 삭제되고_해시가_남아있는_콘텐츠만_정리_대상이다() {
    String deletedWithHash = insertContent("영화E", true, "hash-1");
    String deletedWithoutHash = insertContent("영화F", true, null);
    String aliveWithHash = insertContent("영화G", false, "hash-2");

    List<String> targets = mapper.findDeletedContentIdsNeedingCleanup(10);

    assertThat(targets).contains(deletedWithHash)
        .doesNotContain(deletedWithoutHash, aliveWithHash);
  }

  @Test
  void clearEmbeddingSourceHashBulk는_주어진_콘텐츠들의_해시를_모두_비운다() {
    String first = insertContent("영화H", true, "hash-3");
    String second = insertContent("영화H2", true, "hash-4");

    mapper.clearEmbeddingSourceHashBulk(List.of(first, second));

    assertThat(jdbcTemplate.queryForObject(
        "SELECT embedding_source_hash FROM contents WHERE id = UUID_TO_BIN(?)",
        String.class, first)).isNull();
    assertThat(jdbcTemplate.queryForObject(
        "SELECT embedding_source_hash FROM contents WHERE id = UUID_TO_BIN(?)",
        String.class, second)).isNull();
  }

  @Test
  void offset과_limit으로_콘텐츠id순_페이지네이션이_동작한다() {
    String contentA = insertContent("영화I", false, null);
    String contentB = insertContent("영화J", false, null);

    List<ContentEmbeddingTarget> firstPage = mapper.findContentsNeedingEmbedding(0, 1);
    List<ContentEmbeddingTarget> secondPage = mapper.findContentsNeedingEmbedding(1, 1);

    List<String> expectedOrder = java.util.stream.Stream.of(contentA, contentB).sorted().toList();

    assertThat(firstPage).hasSize(1);
    assertThat(secondPage).hasSize(1);
    assertThat(List.of(firstPage.get(0).contentId(), secondPage.get(0).contentId()))
        .containsExactlyElementsOf(expectedOrder);
  }

  @Test
  void 유형_개봉연도_제작국가를_함께_반환한다() {
    String contentId = insertContent("영화K", false, null);
    jdbcTemplate.update(
        "UPDATE contents SET release_date = '2019-05-30', origin_country = 'KR' WHERE id = UUID_TO_BIN(?)",
        contentId);

    ContentEmbeddingTarget target = findTarget(contentId);

    assertThat(target.type()).isEqualTo("MOVIE");
    assertThat(target.releaseYear()).isEqualTo(2019);
    assertThat(target.originCountry()).isEqualTo("KR");
  }

  @Test
  void 개봉일과_국가가_없으면_해당_값은_null이다() {
    String contentId = insertContent("영화L", false, null);

    ContentEmbeddingTarget target = findTarget(contentId);

    assertThat(target.releaseYear()).isNull();
    assertThat(target.originCountry()).isNull();
  }

  @Test
  void 감독과_출연진은_표시순서대로_반환하고_출연진은_상위_5명만_담는다() {
    String contentId = insertContent("영화M", false, null);
    insertPerson(contentId, "DIRECTOR", "감독A", 0);
    for (int i = 0; i < 7; i++) {
      insertPerson(contentId, "ACTOR", "배우" + i, i + 1);
    }

    ContentEmbeddingTarget target = findTarget(contentId);

    assertThat(target.directors()).isEqualTo("감독A");
    assertThat(target.actors()).isEqualTo("배우0,배우1,배우2,배우3,배우4");
  }

  @Test
  void 태그와_인물이_함께_있어도_값이_중복되지_않는다() {
    String contentId = insertContent("영화N", false, null);
    attachTag(contentId, "Drama");
    attachTag(contentId, "Action");
    insertPerson(contentId, "DIRECTOR", "감독B", 0);
    insertPerson(contentId, "ACTOR", "배우X", 1);
    insertPerson(contentId, "ACTOR", "배우Y", 2);

    ContentEmbeddingTarget target = findTarget(contentId);

    assertThat(target.tagNames()).isEqualTo("Action,Drama");
    assertThat(target.directors()).isEqualTo("감독B");
    assertThat(target.actors()).isEqualTo("배우X,배우Y");
  }

  @Test
  void 인물이_없으면_감독과_출연진은_null이다() {
    String contentId = insertContent("영화O", false, null);

    ContentEmbeddingTarget target = findTarget(contentId);

    assertThat(target.directors()).isNull();
    assertThat(target.actors()).isNull();
  }

  @Test
  void 스포츠_상세정보를_함께_반환하고_일반_콘텐츠는_null이다() {
    String sportsId = insertContent("경기A", false, null);
    jdbcTemplate.update(
        "INSERT INTO content_sports (content_id, sport_type, league, season, home_team, away_team, venue, kickoff_at) "
            + "VALUES (UUID_TO_BIN(?), 'Soccer', 'Premier League', '2025-2026', 'Man Utd', 'Arsenal', 'Old Trafford', "
            + "CURRENT_TIMESTAMP(6))",
        sportsId);
    String movieId = insertContent("영화P", false, null);

    ContentEmbeddingTarget sports = findTarget(sportsId);
    ContentEmbeddingTarget movie = findTarget(movieId);

    assertThat(sports.sportType()).isEqualTo("Soccer");
    assertThat(sports.league()).isEqualTo("Premier League");
    assertThat(sports.season()).isEqualTo("2025-2026");
    assertThat(sports.homeTeam()).isEqualTo("Man Utd");
    assertThat(sports.awayTeam()).isEqualTo("Arsenal");
    assertThat(sports.venue()).isEqualTo("Old Trafford");
    assertThat(movie.sportType()).isNull();
    assertThat(movie.homeTeam()).isNull();
  }

  private ContentEmbeddingTarget findTarget(String contentId) {
    return mapper.findContentsNeedingEmbedding(0, 100).stream()
        .filter(target -> target.contentId().equals(contentId))
        .findFirst()
        .orElseThrow();
  }

  private void insertPerson(String contentId, String roleType, String name, int displayOrder) {
    jdbcTemplate.update(
        "INSERT INTO content_people (id, content_id, role_type, person_name, display_order) "
            + "VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), ?, ?, ?)",
        UUID.randomUUID().toString(), contentId, roleType, name, displayOrder);
  }

  private String insertContent(String title, boolean deleted, String embeddingSourceHash) {
    String contentId = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO contents (id, type, title, deleted_at, embedding_source_hash) "
            + "VALUES (UUID_TO_BIN(?), 'MOVIE', ?, ?, ?)",
        contentId, title, deleted ? java.sql.Timestamp.from(java.time.Instant.now()) : null, embeddingSourceHash);
    return contentId;
  }

  private void attachTag(String contentId, String tagName) {
    String tagId = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO tags (id, name, kind) VALUES (UUID_TO_BIN(?), ?, 'GENRE')",
        tagId, tagName);
    jdbcTemplate.update(
        "INSERT INTO content_tags (tag_id, content_id, source) VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), 'OPENAPI')",
        tagId, contentId);
  }
}
