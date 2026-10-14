package ru.shim.closedclub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
class ClosedclubApplicationTests {

    @DisplayName("Контекст приложения успешно запускается")
    @Test
    void contextLoads() {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("closedclub_test")
                    .withUsername("test")
                    .withPassword("test");
        }
    }
}