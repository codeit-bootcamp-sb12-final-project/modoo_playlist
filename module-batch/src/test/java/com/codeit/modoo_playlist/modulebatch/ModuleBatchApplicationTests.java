package com.codeit.modoo_playlist.modulebatch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:tc:mysql:8.4.7:///modoo_mysql",
		"spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
		"spring.datasource.username=test",
		"spring.datasource.password=test",
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.sql.init.mode=always",
		"spring.sql.init.schema-locations=file:../infra/src/main/resources/schema.sql",
		"spring.batch.jdbc.initialize-schema=always",
		"spring.data.elasticsearch.repositories.enabled=false",
		"batch.embedding.enabled=false",
		"batch.recommendation.enabled=false",
		"batch.tmdb.enabled=false",
		"batch.sports.enabled=false",
		"batch.sports.status-update-enabled=false",
		"external.tmdb.access-token=test",
		"external.sports-db.api-key=test",
		"spring.ai.google.genai.api-key=test",
		"server.port=0"
})
class ModuleBatchApplicationTests {

	@Test
	void contextLoads() {
	}

}
