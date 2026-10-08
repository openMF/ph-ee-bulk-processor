package org.mifos.processor.bulk.api.implementation;

import static org.mifos.processor.bulk.camel.config.CamelProperties.CALLBACK;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PROGRAM_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PAYEE_DFSP_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.HEADER_CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.HEADER_PLATFORM_TENANT_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.HEADER_TYPE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import io.camunda.zeebe.client.api.command.ClientStatusException;
import io.grpc.Status;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.mifos.connector.common.interceptor.JWSUtil;
import org.mifos.processor.bulk.api.definition.BatchTransactions;
import org.mifos.processor.bulk.file.FileStorageService;
import org.mifos.processor.bulk.format.RestRequestConvertor;
import org.mifos.processor.bulk.schema.BatchRequestDTO;
import org.mifos.processor.bulk.schema.CamelApiResponse;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.utility.CsvWriter;
import org.mifos.processor.bulk.utility.Headers;
import org.mifos.processor.bulk.utility.SpringWrapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartException;

@Slf4j
@RestController
public class BatchTransactionsController implements BatchTransactions {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    FileStorageService fileStorageService;

    @Autowired
    RestRequestConvertor restRequestConvertor;

    @Value("#{'${tenants}'.split(',')}")
    protected List<String> tenants;
    @Autowired
    private CsvMapper csvMapper;

    @SneakyThrows
    @Override
    public String batchTransactions(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String requestId,
            String fileName, String purpose, String type, String tenant, String registeringInstitutionId, String programId,
            String callbackUrl, String payeeDfspId) {

        log.info("Inside api logic");
        Headers.HeaderBuilder headerBuilder = new Headers.HeaderBuilder().addHeader(HEADER_CLIENT_CORRELATION_ID, requestId)
                .addHeader(PURPOSE, purpose).addHeader(HEADER_TYPE, type).addHeader(HEADER_PLATFORM_TENANT_ID, tenant)
                .addHeader(HEADER_REGISTERING_INSTITUTE_ID, registeringInstitutionId).addHeader(HEADER_PROGRAM_ID, programId)
                .addHeader(CALLBACK, callbackUrl).addHeader(PAYEE_DFSP_ID, payeeDfspId);

        Optional<String> validationResponse = isValidRequest(httpServletRequest, fileName, type);
        if (validationResponse.isPresent()) {
            httpServletResponse.setStatus(httpServletResponse.SC_BAD_REQUEST);
            return validationResponse.get();
        }

        if (JWSUtil.isMultipartRequest(httpServletRequest)) {
            log.info("This is file based request");
            // Use Spring's MultipartHttpServletRequest instead of JWSUtil.parseFormData()
            org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest = (org.springframework.web.multipart.MultipartHttpServletRequest) httpServletRequest;
            org.springframework.web.multipart.MultipartFile multipartFile = multipartRequest.getFile("data");

            log.info("multipartFile is null: {}", multipartFile == null);
            if (multipartFile != null) {
                log.info("multipartFile.isEmpty(): {}", multipartFile.isEmpty());
                log.info("multipartFile.getSize(): {}", multipartFile.getSize());
                log.info("multipartFile.getOriginalFilename(): {}", multipartFile.getOriginalFilename());
            }

            if (multipartFile == null || multipartFile.isEmpty()) {
                log.error("No file data found in multipart request");
                httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return getErrorResponse("No file data", "No file was uploaded with the request", HttpServletResponse.SC_BAD_REQUEST);
            }

            String localFileName = fileStorageService.save(multipartFile);
            Headers headers = headerBuilder.addHeader(FILE_NAME, localFileName).build();
            log.info("Headers passed: {}", headers.getHeaders());

            CamelApiResponse response = sendRequestToCamel(headers);
            httpServletResponse.setStatus(response.getStatus());
            return response.getBody();
        } else {
            log.info("This is json based request");
            String jsonString = IOUtils.toString(httpServletRequest.getInputStream(), Charset.defaultCharset());
            List<BatchRequestDTO> batchRequestDTOList = objectMapper.readValue(jsonString, new TypeReference<>() {});
            List<Transaction> transactionList = restRequestConvertor.convertListFrom(batchRequestDTOList);

            String localFileName = UUID.randomUUID() + ".csv";
            CsvWriter.writeToCsv(transactionList, Transaction.class, csvMapper, true, localFileName);
            Headers headers = headerBuilder.addHeader(HEADER_TYPE, "csv").addHeader(FILE_NAME, localFileName).build();

            CamelApiResponse response = sendRequestToCamel(headers);
            httpServletResponse.setStatus(response.getStatus());
            return response.getBody();
        }

    }

    @ExceptionHandler({ MultipartException.class })
    public String handleMultipartException(HttpServletResponse httpServletResponse) {
        httpServletResponse.setStatus(httpServletResponse.SC_BAD_REQUEST);
        return getErrorResponse("File not uploaded", "There was no fie uploaded with the request. " + "Please upload a file and try again.",
                400);
    }

    private CamelApiResponse sendRequestToCamel(Headers headers) {
        Exchange exchange = SpringWrapperUtil.getDefaultWrappedExchange(producerTemplate.getCamelContext(), headers);
        exchange = producerTemplate.send("direct:post-batch-transactions", exchange);
        checkAndThrowClientStatusException(exchange);
        int statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String body = exchange.getIn().getBody(String.class);
        return new CamelApiResponse(body, statusCode);
    }

    private String getErrorResponse(String information, String description, int code) {
        JSONObject json = new JSONObject();
        json.put("errorInformation", "File not uploaded");
        json.put("errorDescription", "There was no fie uploaded with the request. " + "Please upload a file and try again.");
        json.put("errorCode", code);
        return json.toString();
    }

    // validates the request header, and return errorJson string if the request is invalid else an empty optional
    private Optional<String> isValidRequest(HttpServletRequest httpServletRequest, String fileName, String type) {

        Optional<String> response = Optional.empty();
        if ((JWSUtil.isMultipartRequest(httpServletRequest) && !type.equalsIgnoreCase("csv"))
                || (!JWSUtil.isMultipartRequest(httpServletRequest) && !type.equalsIgnoreCase("raw"))) {
            String errorJson = getErrorResponse("Type mismatch",
                    "The value of the header \"" + HEADER_TYPE + "\" doesn't match with the request content-type", 400);
            response = Optional.of(errorJson);

        }
        if (JWSUtil.isMultipartRequest(httpServletRequest) && fileName.isEmpty()) {
            String errorJson = getErrorResponse("Header can't be empty",
                    "If the request is of type csv, the header \"" + FILE_NAME + "\"can't be empty", 400);
            response = Optional.of(errorJson);
        }
        if (!type.equalsIgnoreCase("raw") && !type.equalsIgnoreCase("csv")) {
            String errorJson = getErrorResponse("Invalid TYPE header value passed",
                    "The value of the header \"" + HEADER_TYPE + "\" can be \"[raw,csv]\" but is " + type, 400);
            response = Optional.of(errorJson);
        }
        return response;
    }

    private void checkAndThrowClientStatusException(Exchange exchange) {
        Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        if (cause instanceof ClientStatusException) {
            throw new ClientStatusException(Status.FAILED_PRECONDITION, cause);
        }
    }

    @Override
    public String updateBatchExecution(MultiValueMap<String, Object> executionPayload, String tenant, String requestId) {

        log.info("## BATCH EXECUTION UPDATE - Received execution update request");
        log.info("## Tenant: {}", tenant);
        log.info("## Request ID: {}", requestId);
        log.info("## Payload keys: {}", executionPayload.keySet());
        log.info("## Payload size: {}", executionPayload.size());

        // Log all payload data for debugging
        executionPayload.forEach((key, value) -> {
            log.info("## Execution payload - {}: {}", key, value);
        });

        try {
            // Extract transaction results from payload
            // Expected format: status, transactionId, completedTimestamp, etc.
            String status = getPayloadValue(executionPayload, "status");
            String transactionId = getPayloadValue(executionPayload, "transactionId");
            String batchId = getPayloadValue(executionPayload, "batchId");
            String subBatchId = getPayloadValue(executionPayload, "subBatchId");

            log.info("## Processing execution update - batchId: {}, subBatchId: {}, transactionId: {}, status: {}", batchId, subBatchId,
                    transactionId, status);

            // TODO: Update transaction status in database/Zeebe workflow
            // For now, just acknowledge receipt

            JSONObject response = new JSONObject();
            response.put("message", "Batch execution update received successfully");
            response.put("requestId", requestId);
            response.put("batchId", batchId);
            response.put("transactionId", transactionId);
            response.put("status", "ACCEPTED");

            log.info("## BATCH EXECUTION UPDATE - Successfully processed update for transactionId: {}", transactionId);

            return response.toString();

        } catch (Exception e) {
            log.error("## BATCH EXECUTION UPDATE - Error processing execution update", e);
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", "Failed to process execution update");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("requestId", requestId);
            return errorResponse.toString();
        }
    }

    private String getPayloadValue(MultiValueMap<String, Object> payload, String key) {
        List<Object> values = payload.get(key);
        if (values != null && !values.isEmpty()) {
            Object value = values.get(0);
            return value != null ? value.toString() : null;
        }
        return null;
    }
}
