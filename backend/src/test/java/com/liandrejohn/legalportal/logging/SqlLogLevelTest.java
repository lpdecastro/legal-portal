package com.liandrejohn.legalportal.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * Starts a bare context, without auto-configuration or a database, so only application.yaml (and the repo-root .env)
 * apply logging levels.
 */
class SqlLogLevelTest {

    private static final String SQL_LOGGER = "org.hibernate.SQL";

    @AfterEach
    void resetLevel() {
        // Logging levels are JVM-wide; don't leave DEBUG on for later test classes
        LoggingSystem.get(getClass().getClassLoader()).setLogLevel(SQL_LOGGER, null);
    }

    @Test
    void sqlIsNotLoggedByDefault() {
        try (ConfigurableApplicationContext context = start()) {
            assumeTrue(context.getEnvironment().getProperty("SQL_LOG_LEVEL") == null,
                    "SQL_LOG_LEVEL is set in the local .env");
            assertThat(LoggerFactory.getLogger(SQL_LOGGER).isDebugEnabled()).isFalse();
        }
    }

    @Test
    void debugLevelLogsSql() {
        try (ConfigurableApplicationContext context = start("--SQL_LOG_LEVEL=DEBUG")) {
            assertThat(LoggerFactory.getLogger(SQL_LOGGER).isDebugEnabled()).isTrue();
        }
    }

    private static ConfigurableApplicationContext start(String... args) {
        return new SpringApplicationBuilder(EmptyConfig.class).web(WebApplicationType.NONE).run(args);
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfig {

    }

}
