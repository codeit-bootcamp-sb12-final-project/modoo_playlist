package com.codeit.modoo_playlist.moduleapi.domain.tag.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.codeit.modoo_playlist.moduleapi.domain.tag.repository.jpa.TagRepository;

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
@ContextConfiguration(classes = TagServiceTransactionMySqlTest.JpaTestConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TagServiceTransactionMySqlTest {

    @Autowired private TagRepository tagRepository;
    @Autowired private TransactionRunner transactionRunner;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.codeit.modoo_playlist.core")
    @EnableJpaRepositories(basePackages = "com.codeit.modoo_playlist.moduleapi.domain.tag")
    static class JpaTestConfiguration {

        @Bean
        TagServiceImpl tagService(TagRepository tagRepository) {
            return new TagServiceImpl(tagRepository);
        }

        @Bean
        TransactionRunner transactionRunner(TagServiceImpl tagService) {
            return new TransactionRunner(tagService);
        }
    }

    static class TransactionRunner {

        private final TagServiceImpl tagService;

        TransactionRunner(TagServiceImpl tagService) {
            this.tagService = tagService;
        }

        @Transactional
        void createAndCommit(String name) {
            tagService.getOrCreateTags(List.of(name));
        }

        @Transactional
        void createAndRollback(String name) {
            tagService.getOrCreateTags(List.of(name));
            throw new IllegalStateException("rollback");
        }
    }

    @Test
    void 상위_트랜잭션이_커밋되면_생성한_태그도_저장된다() {
        transactionRunner.createAndCommit("Committed Tag");

        assertThat(tagRepository.findAllByNameIn(List.of("Committed Tag")))
                .extracting("name")
                .containsExactly("Committed Tag");
    }

    @Test
    void 상위_트랜잭션이_롤백되면_생성한_태그도_롤백된다() {
        assertThatThrownBy(() -> transactionRunner.createAndRollback("Rolled Back Tag"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(tagRepository.findAllByNameIn(List.of("Rolled Back Tag"))).isEmpty();
    }
}
