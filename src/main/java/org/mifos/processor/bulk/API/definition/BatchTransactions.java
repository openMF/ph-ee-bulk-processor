package org.mifos.processor.bulk.api.definition;

<<<<<<< HEAD
import com.amazonaws.services.dynamodbv2.xspec.S;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.camel.util.json.JsonObject;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.Multipart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PLATFORM_TENANT_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;
=======
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
>>>>>>> b898933 (PHEE-307 Resolve checkstyle errors manually and update gradle command in CI)

public interface BatchTransactions {

    @PostMapping(value = "/batchtransactions", produces = "application/json")
<<<<<<< HEAD
    String batchTransactions(
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse,
            @RequestHeader(value = HEADER_CLIENT_CORRELATION_ID) String requestId,
            @RequestHeader(value = FILE_NAME, required = false) String fileName,
            @RequestHeader(value = PURPOSE) String purpose,
            @RequestHeader(value = HEADER_TYPE) String type,
            @RequestHeader(value = HEADER_PLATFORM_TENANT_ID) String tenant,
            @RequestHeader(value = HEADER_REGISTERING_INSTITUTE_ID, required = false) String registeringInstitutionId,
            @RequestHeader(value = HEADER_PROGRAM_ID, required = false) String programId) throws IOException;

=======
    String batchTransactions(HttpServletResponse httpServletResponse, @RequestHeader(value = "X-CorrelationID") String requestId,
            @RequestParam("data") MultipartFile file, @RequestHeader(value = FILE_NAME) String fileName,
            @RequestHeader(value = PURPOSE) String purpose, @RequestHeader(value = "Type") String type,
            @RequestHeader(value = "Platform-TenantId") String tenant) throws IOException;
>>>>>>> b898933 (PHEE-307 Resolve checkstyle errors manually and update gradle command in CI)
}
