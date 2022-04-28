package org.mifos.processor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class BulkProcessorApplicationTests {

    @Test
    void contextLoads() {
        System.out.println(UUID.randomUUID().toString());
    }

}
