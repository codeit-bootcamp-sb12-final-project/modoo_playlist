package com.codeit.modoo_playlist.modulebatch.sports.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.codeit.modoo_playlist.modulebatch.sports.model.SportsSyncContent;

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
@ContextConfiguration(classes = SportsContentMapperMySqlTest.MyBatisTestConfiguration.class)
class SportsContentMapperMySqlTest {

    @Autowired private SportsContentMapper mapper;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @MapperScan("com.codeit.modoo_playlist.modulebatch.sports.persistence")
    static class MyBatisTestConfiguration {
    }

    @Test
    void 팀_로고를_저장하고_다시_upsert하면_갱신한다() {
        SportsSyncContent first = content("event-1", "home-1.png", "away-1.png");
        mapper.upsertContent(first);
        String storedId = mapper.findContentIdBySourceId("event-1");
        mapper.upsertSports(storedId, first.sports());

        assertThat(mapper.findBySourceId("event-1")).satisfies(saved -> {
            assertThat(saved.homeTeamBadge()).isEqualTo("home-1.png");
            assertThat(saved.awayTeamBadge()).isEqualTo("away-1.png");
        });

        SportsSyncContent changed = content("event-1", "home-2.png", "away-2.png");
        mapper.upsertContent(changed);
        mapper.upsertSports(storedId, changed.sports());

        assertThat(mapper.findContentIdBySourceId("event-1")).isEqualTo(storedId);
        assertThat(mapper.findBySourceId("event-1")).satisfies(saved -> {
            assertThat(saved.homeTeamBadge()).isEqualTo("home-2.png");
            assertThat(saved.awayTeamBadge()).isEqualTo("away-2.png");
        });
    }

    @Test
    void 로고가_없는_경기도_null로_저장하고_조회한다() {
        SportsSyncContent content = content("event-2", null, null);
        mapper.upsertContent(content);
        String storedId = mapper.findContentIdBySourceId("event-2");
        mapper.upsertSports(storedId, content.sports());

        assertThat(mapper.findBySourceId("event-2")).satisfies(saved -> {
            assertThat(saved.homeTeamBadge()).isNull();
            assertThat(saved.awayTeamBadge()).isNull();
            assertThat(saved.homeTeam()).isEqualTo("Arsenal");
        });
    }

    private SportsSyncContent content(String sourceId, String homeTeamBadge, String awayTeamBadge) {
        var sports = new SportsSyncContent.Sports("Soccer", "Premier League", "2026",
                "Arsenal", "Chelsea", "Stadium", homeTeamBadge, awayTeamBadge,
                "SCHEDULED", Instant.parse("2026-12-01T12:00:00Z"));
        return new SportsSyncContent(UUID.randomUUID().toString(), "Arsenal vs Chelsea", null, null,
                sourceId, LocalDate.of(2026, 12, 1), "England", sports, List.of());
    }
}
