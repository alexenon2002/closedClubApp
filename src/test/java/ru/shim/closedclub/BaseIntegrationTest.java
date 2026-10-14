package ru.shim.closedclub;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.shim.closedclub.repository.MemberRepository;
import ru.shim.closedclub.repository.QrCodeRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BaseIntegrationTest.ContainerConfiguration.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected QrCodeRepository qrCodeRepository;

    @BeforeEach
    protected void clearDatabase() {
        qrCodeRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class ContainerConfiguration {

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