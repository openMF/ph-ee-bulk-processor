package org.mifos.processor.bulk.api.definition;

import com.amazonaws.services.dynamodbv2.xspec.S;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.camel.util.json.JsonObject;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.Multipart;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

public interface BatchTransactions {

    @PostMapping(value = "/batchtransactions", produces="application/json")
    String batchTransactions(HttpServletResponse httpServletResponse, @RequestHeader(value = "X-CorrelationID") String requestId,
                             @RequestParam("data") MultipartFile file,
                             @RequestHeader(value = FILE_NAME) String fileName,
                             @RequestHeader(value = PURPOSE) String purpose,
                             @RequestHeader(value = "Type") String type,
                             @RequestHeader(value = "Platform-TenantId") String tenant) throws IOException;
}
