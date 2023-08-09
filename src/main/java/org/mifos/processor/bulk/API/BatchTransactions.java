package org.mifos.processor.bulk.api;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;

import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

public interface BatchTransactions {

    @PostMapping(value = "/batchtransactions", produces = "application/json")
    String batchTransactions(@RequestHeader(value = "X-CorrelationID") String requestId, @RequestParam("data") MultipartFile file,
            @RequestHeader(value = FILE_NAME) String fileName, @RequestHeader(value = PURPOSE) String purpose,
            @RequestHeader(value = "Type") String type, @RequestHeader(value = "Platform-TenantId") String tenant) throws IOException;
}
