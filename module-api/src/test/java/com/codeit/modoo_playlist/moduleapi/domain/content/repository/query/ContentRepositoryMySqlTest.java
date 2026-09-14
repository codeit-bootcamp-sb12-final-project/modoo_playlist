package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentListCondition.SortDirection;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentListCondition.SortType;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:tc:mysql:8.4.7:///modoo_mysql",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../infra/src/main/resources/schema.sql",
        "spring.test.database.replace=NONE"
})
@ContextConfiguration(classes = ContentRepositoryMySqlTest.JpaTestConfiguration.class)
class ContentRepositoryMySqlTest {

    @Autowired private EntityManager entityManager;
    @Autowired private ContentRepository contentRepository;
    @Autowired private ContentTagRepository contentTagRepository;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.codeit.modoo_playlist.core")
    @EnableJpaRepositories(basePackages = "com.codeit.modoo_playlist.moduleapi.domain.content")
    static class JpaTestConfiguration {
    }

    @Test
    void 타입_제목_태그OR_조건과_소프트삭제를_함께_적용한다() {
        Tag action = persistTag("Action");
        Tag drama = persistTag("Drama");
        Content target = persistContent("Action Movie", ContentType.MOVIE, "4.0", 1);
        Content second = persistContent("Drama Movie", ContentType.MOVIE, "3.0", 2);
        Content tv = persistContent("Action Series", ContentType.TV, "5.0", 3);
        Content deleted = persistContent("Deleted Movie", ContentType.MOVIE, "5.0", 4);
        deleted.softDelete();
        link(target, action);
        link(second, drama);
        link(tv, action);
        link(deleted, action);
        flushAndClear();

        ContentQueryPage result = contentRepository.findAllByCondition(new ContentListCondition(
                ContentType.MOVIE, "movie", List.of("Action", "Drama"), null, null,
                20, SortType.CREATED_AT, SortDirection.ASCENDING
        ));

        assertThat(result.contents()).extracting(item -> item.content().getTitle())
                .containsExactly("Action Movie", "Drama Movie");
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @Test
    void 평점_커서_페이지는_중복과_누락_없이_다음_페이지를_조회한다() {
        Content first = persistContent("first", ContentType.MOVIE, "5.0", 1);
        Content second = persistContent("second", ContentType.MOVIE, "4.0", 2);
        Content third = persistContent("third", ContentType.MOVIE, "3.0", 3);
        flushAndClear();

        ContentQueryPage firstPage = contentRepository.findAllByCondition(condition(
                null, null, 2, SortType.AVERAGE_RATING, SortDirection.DESCENDING
        ));
        ContentQueryPage secondPage = contentRepository.findAllByCondition(condition(
                firstPage.nextCursor(), firstPage.nextIdAfter(), 2,
                SortType.AVERAGE_RATING, SortDirection.DESCENDING
        ));

        assertThat(firstPage.contents()).extracting(item -> item.content().getId())
                .containsExactly(first.getId(), second.getId());
        assertThat(secondPage.contents()).extracting(item -> item.content().getId())
                .containsExactly(third.getId());
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 태그_연결을_fetchJoin으로_조회하고_contentCount를_안전하게_증감한다() {
        Tag tag = persistTag("Action");
        Content content = persistContent("Movie", ContentType.MOVIE, "0.0", 1);
        link(content, tag);
        flushAndClear();

        List<ContentTag> links = contentTagRepository.findAllWithTagByContentIds(List.of(content.getId()));
        contentTagRepository.increaseTagContentCounts(List.of(tag.getId()));
        contentTagRepository.decreaseTagContentCounts(List.of(tag.getId()));
        contentTagRepository.decreaseTagContentCounts(List.of(tag.getId()));
        flushAndClear();

        assertThat(links).singleElement().satisfies(link ->
                assertThat(link.getTag().getName()).isEqualTo("Action"));
        assertThat(entityManager.find(Tag.class, tag.getId()).getContentCount()).isZero();
    }

    @Test
    void 유사콘텐츠는_공유태그가_많은_순서로_조회하고_삭제콘텐츠는_제외한다() {
        Tag action = persistTag("Action");
        Tag drama = persistTag("Drama");
        Content target = persistContent("target", ContentType.MOVIE, "0.0", 1);
        Content closest = persistContent("closest", ContentType.MOVIE, "0.0", 2);
        Content partial = persistContent("partial", ContentType.MOVIE, "0.0", 3);
        Content deleted = persistContent("deleted", ContentType.MOVIE, "0.0", 4);
        deleted.softDelete();
        link(target, action); link(target, drama);
        link(closest, action); link(closest, drama);
        link(partial, action); link(deleted, action); link(deleted, drama);
        flushAndClear();

        var result = contentTagRepository.findSimilarContents(target.getId(), PageRequest.of(0, 10));

        assertThat(result).extracting(item -> item.title()).containsExactly("closest", "partial");
        assertThat(result).extracting(item -> item.score()).containsExactly(2.0, 1.0);
    }

    private ContentListCondition condition(
            String cursor, java.util.UUID idAfter, int limit, SortType sort, SortDirection direction
    ) {
        return new ContentListCondition(null, null, List.of(), cursor, idAfter, limit, sort, direction);
    }

    private Content persistContent(String title, ContentType type, String rating, int seconds) {
        Instant timestamp = Instant.parse("2026-09-14T00:00:00Z").plusSeconds(seconds);
        Content content = Content.builder()
                .type(type).title(title).averageRating(new BigDecimal(rating))
                .createdAt(timestamp).updatedAt(timestamp).build();
        entityManager.persist(content);
        entityManager.flush();
        return content;
    }

    private Tag persistTag(String name) {
        Tag tag = Tag.builder().name(name).kind(TagKind.GENRE)
                .createdAt(Instant.parse("2026-09-14T00:00:00Z")).build();
        entityManager.persist(tag);
        entityManager.flush();
        return tag;
    }

    private void link(Content content, Tag tag) {
        entityManager.persist(ContentTag.builder()
                .id(new ContentTagId(content.getId(), tag.getId()))
                .content(content).tag(tag).build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
