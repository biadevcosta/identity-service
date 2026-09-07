package com.biadevcosta.identity.support;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Shared singleton MySQL container, started once for the test run and left to Ryuk to reap.
 * Properties are bound with {@link DynamicPropertySource}. When no Docker daemon is reachable the
 * whole integration test is skipped (assumption failure) instead of failing the build.
 */
public abstract class AbstractIntegrationTest {

    private static final boolean DOCKER_AVAILABLE = isDockerAvailable();

    protected static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    static {
        if (DOCKER_AVAILABLE) {
            MYSQL.start();
        }
    }

    @BeforeAll
    static void requireDocker() {
        assumeTrue(DOCKER_AVAILABLE, "Docker is not available - skipping integration test");
    }

    private static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}
