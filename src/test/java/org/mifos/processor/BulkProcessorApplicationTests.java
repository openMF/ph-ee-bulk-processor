package org.mifos.processor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
class BulkProcessorApplicationTests {

    @Test
    void contextLoads() {
        System.out.println(UUID.randomUUID());
    }

}
