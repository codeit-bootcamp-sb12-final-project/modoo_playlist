package com.codeit.modoo_playlist.modulebatch.tmdb.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbSyncContent;

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
@ContextConfiguration(classes = TmdbContentMapperMySqlTest.MyBatisTestConfiguration.class)
class TmdbContentMapperMySqlTest {

    @Autowired private TmdbContentMapper mapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @MapperScan("com.codeit.modoo_playlist.modulebatch.tmdb.persistence")
    static class MyBatisTestConfiguration {
    }

    @Test
    void sourceTypeSourceId_기준으로_콘텐츠와_비디오를_멱등_갱신한다() {
        TmdbSyncContent first = content(UUID.randomUUID().toString(), "old title", "10", List.of());
        mapper.upsertContent(first);
        String storedId = mapper.findContentIdBySourceId("MOVIE", "10");
        mapper.upsertVideo(storedId, first.video());

        TmdbSyncContent changed = content(UUID.randomUUID().toString(), "new title", "10", List.of());
        mapper.upsertContent(changed);
        mapper.upsertVideo(storedId, new TmdbSyncContent.Video(
                130, "series", "tt10", "RELEASED", null, null,
                "en", 20f, new BigDecimal("8.5"), 1000));

        assertThat(mapper.findContentIdBySourceId("MOVIE", "10")).isEqualTo(storedId);
        assertThat(mapper.findBySourceId("MOVIE", "10")).satisfies(saved -> {
            assertThat(saved.title()).isEqualTo("new title");
            assertThat(saved.runtimeMinutes()).isEqualTo(130);
            assertThat(saved.externalRating()).isEqualByComparingTo("8.5");
        });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM contents WHERE source='TMDB' AND type='MOVIE' AND source_id='10'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void OPENAPI_태그_diff는_연결과_contentCount를_맞추고_LLM태그는_보존한다() {
        TmdbSyncContent saved = content(UUID.randomUUID().toString(), "movie", "20", List.of());
        mapper.upsertContent(saved);
        String contentId = mapper.findContentIdBySourceId("MOVIE", "20");

        List<TmdbSyncContent.Tag> initial = List.of(tag("Action"), tag("Drama"));
        mapper.insertTags(initial);
        mapper.increaseNewContentTagCounts(contentId, initial);
        mapper.upsertContentTags(contentId, initial);
        insertLlmTag(contentId, "Personal");

        List<TmdbSyncContent.Tag> next = List.of(tag("Drama"));
        mapper.insertTags(next);
        mapper.decreaseMissingOpenApiTagCounts(contentId, next);
        mapper.deleteMissingOpenApiTags(contentId, next);
        mapper.increaseNewContentTagCounts(contentId, next);
        mapper.upsertContentTags(contentId, next);

        assertThat(tagCount("Action")).isZero();
        assertThat(tagCount("Drama")).isEqualTo(1);
        assertThat(tagCount("Personal")).isEqualTo(1);
        assertThat(mapper.findTagsByContentId(contentId))
                .extracting(tag -> tag.name() + ":" + tag.source())
                .containsExactly("Drama:OPENAPI", "Personal:LLM");
    }

    @Test
    void 출연진은_전체삭제후_새목록으로_교체한다() {
        TmdbSyncContent saved = content(UUID.randomUUID().toString(), "movie", "30", List.of());
        mapper.upsertContent(saved);
        String contentId = mapper.findContentIdBySourceId("MOVIE", "30");
        mapper.insertPeople(contentId, List.of(person("old", 0), person("remove", 1)));

        mapper.deletePeople(contentId);
        mapper.insertPeople(contentId, List.of(person("new", 0)));

        assertThat(mapper.findPeopleByContentId(contentId))
                .extracting(person -> person.personName())
                .containsExactly("new");
    }

    private TmdbSyncContent content(String id, String title, String sourceId, List<TmdbSyncContent.Tag> tags) {
        return new TmdbSyncContent(
                id, "MOVIE", title, "description", "https://image", sourceId,
                LocalDate.of(2026, 9, 14), "US",
                new TmdbSyncContent.Video(120, null, "tt" + sourceId, "RELEASED",
                        null, null, "en", 10f, new BigDecimal("7.0"), 100),
                List.of(), tags, true, true);
    }

    private TmdbSyncContent.Tag tag(String name) {
        return new TmdbSyncContent.Tag(UUID.randomUUID().toString(), name, "GENRE");
    }

    private TmdbSyncContent.Person person(String name, int order) {
        return new TmdbSyncContent.Person(UUID.randomUUID().toString(), "CAST", name, null,
                order, "person-" + name, null);
    }

    private void insertLlmTag(String contentId, String name) {
        String tagId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO tags(id, name, kind, content_count, created_at) "
                        + "VALUES(UUID_TO_BIN(?), ?, 'KEYWORD', 1, CURRENT_TIMESTAMP(6))",
                tagId, name);
        jdbcTemplate.update(
                "INSERT INTO content_tags(tag_id, content_id, source) "
                        + "VALUES(UUID_TO_BIN(?), UUID_TO_BIN(?), 'LLM')",
                tagId, contentId);
    }

    private int tagCount(String name) {
        return jdbcTemplate.queryForObject("SELECT content_count FROM tags WHERE name = ?", Integer.class, name);
    }
}
