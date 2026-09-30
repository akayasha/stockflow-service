package com.stockflow.testsupport;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots a single Postgres container for the entire test JVM and injects the
 * JDBC URL / credentials into the Spring environment. Containers are reused
 * across test classes via {@link #CONTAINER} so the suite stays fast.
 *
 * <p>The container is only started when a Docker daemon is reachable. In
 * environments without Docker (CI sandboxes, Windows without Docker Desktop,
 * etc.) the initializer is a no-op - tests must then be opted into via
 * {@code RUN_INTEGRATION_TESTS=true}.
 */
public final class PostgresTestContainer {

    public static final String INTEGRATION_FLAG = "RUN_INTEGRATION_TESTS";

    public static final boolean DOCKER_AVAILABLE =
        DockerClientFactory.instance().isDockerAvailable();

    public static final PostgreSQLContainer<?> CONTAINER;

    static {
        if (DOCKER_AVAILABLE) {
            CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("stockflow_test")
                .withUsername("stockflow")
                .withPassword("stockflow")
                .withReuse(true);
            CONTAINER.start();
        } else {
            CONTAINER = null;
        }
    }

    private PostgresTestContainer() {
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            if (!DOCKER_AVAILABLE || CONTAINER == null) {
                return;
            }
            TestPropertyValues.of(
                "spring.datasource.url=" + CONTAINER.getJdbcUrl(),
                "spring.datasource.username=" + CONTAINER.getUsername(),
                "spring.datasource.password=" + CONTAINER.getPassword(),
                "stockflow.app.seed-demo-data=false"
            ).applyTo(context.getEnvironment());
        }
    }
}
