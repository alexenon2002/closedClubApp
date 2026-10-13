package ru.shim.closedclub;

import org.springframework.context.annotation.Import;
import ru.shim.closedclub.config.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class ClosedclubApplicationTests {

    @Test
    void contextLoads() {
    }

}
