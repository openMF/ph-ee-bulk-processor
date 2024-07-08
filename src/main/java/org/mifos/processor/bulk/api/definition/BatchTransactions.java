package org.mifos.processor.bulk.api.definition;

import static org.mifos.processor.bulk.camel.config.CamelProperties.CALLBACK;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PLATFORM_TENANT_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PROGRAM_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PAYEE_DFSP_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.HEADER_TYPE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

public interface BatchTransactions {

    @PostMapping(value = "/batchtransactions", produces = "application/json")
    String batchTransactions(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            @RequestHeader(value = HEADER_CLIENT_CORRELATION_ID) String requestId,
            @RequestHeader(value = FILE_NAME, required = false) String fileName, @RequestHeader(value = PURPOSE) String purpose,
            @RequestHeader(value = HEADER_TYPE) String type, @RequestHeader(value = HEADER_PLATFORM_TENANT_ID) String tenant,
            @RequestHeader(value = HEADER_REGISTERING_INSTITUTE_ID, required = false) String registeringInstitutionId,
            @RequestHeader(value = HEADER_PROGRAM_ID, required = false) String programId,
            @RequestHeader(value = CALLBACK, required = false) String callbackUrl,
            @RequestHeader(value = PAYEE_DFSP_ID, required = false) String payeeDfspId) throws IOException;

}
