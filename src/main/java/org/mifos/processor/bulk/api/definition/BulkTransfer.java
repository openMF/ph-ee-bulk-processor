package org.mifos.processor.bulk.api.definition;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;

import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

public interface BulkTransfer {

    @Deprecated

    @PostMapping(value = "/bulk/transfer/{requestId}/{fileName}", produces = "application/json")
    String bulkTransfer(@RequestHeader(value = "X-CorrelationID", required = false) String requestId,
            @RequestParam("data") MultipartFile file, @RequestHeader(value = FILE_NAME, required = false) String fileName,
            @RequestHeader(value = PURPOSE, required = false) String purpose, @RequestHeader(value = "Type", required = false) String type,
            @RequestHeader(value = "Platform-TenantId") String tenant) throws IOException;
}
