package com.acousticguard.hub;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base integration test class using Testcontainers for PostgreSQL (PostGIS) and RabbitMQ.
 * Uses the Singleton Container pattern to ensure containers are started only once per test suite.
 * 
 * This class provides:
 * - PostgreSQL with PostGIS extension for spatial queries
 * - RabbitMQ for AMQP integration testing
 * - Dynamic property configuration for Spring Boot
 */
@SpringBootTest
public abstract class BaseIntegrationTest {

    // Singleton PostgreSQL container with PostGIS support
    static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.3").asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("acoustic_guard_test")
            .withUsername("test")
            .withPassword("test");

    // Singleton RabbitMQ container
    static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.12-management")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure PostgreSQL datasource
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Configure RabbitMQ
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        // Configure Hibernate for PostGIS dialect
        registry.add("spring.jpa.properties.hibernate.dialect", 
                () -> "org.hibernate.spatial.dialect.postgis.PostgisDialect");
        
        // Disable Liquibase for tests to avoid migration conflicts
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @BeforeAll
    static void startContainers() {
        // Start containers only once per test suite
        if (!postgresContainer.isRunning()) {
            postgresContainer.start();
        }
        if (!rabbitMQContainer.isRunning()) {
            rabbitMQContainer.start();
        }
    }
}
