package org.mifos.processor;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BulkProcessorApplicationTests {

    @Test
    void contextLoads() {
        System.out.println(UUID.randomUUID());
    }

}
