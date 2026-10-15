package com.liandrejohn.legalportal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PostgresContainerIntegrationTest {

	@Autowired
	private PostgreSQLContainer postgresContainer;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void datasourceConnectsToTestcontainersPostgres() {
		assertThat(postgresContainer.isRunning()).isTrue();

		String databaseName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
		Integer serverVersion = jdbcTemplate.queryForObject("SHOW server_version_num", Integer.class);

		assertThat(databaseName).isEqualTo(postgresContainer.getDatabaseName());
		assertThat(serverVersion).isBetween(170000, 179999);
	}

}
