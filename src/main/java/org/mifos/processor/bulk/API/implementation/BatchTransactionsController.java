package org.mifos.processor.bulk.api.implementation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PROGRAM_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.HEADER_CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.HEADER_PLATFORM_TENANT_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.HEADER_TYPE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;

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
    public String batchTransactions(
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse,
            String requestId,
            String fileName,
            String purpose,
            String type,
            String tenant,
            String registeringInstitutionId,
            String programId) {

        log.info("Inside api logic");
        Headers.HeaderBuilder headerBuilder = new Headers.HeaderBuilder()
                .addHeader(HEADER_CLIENT_CORRELATION_ID, requestId)
                .addHeader(PURPOSE, purpose)
                .addHeader(HEADER_TYPE, type)
                .addHeader(HEADER_PLATFORM_TENANT_ID, tenant)
                .addHeader(HEADER_REGISTERING_INSTITUTE_ID, registeringInstitutionId)
                .addHeader(HEADER_PROGRAM_ID, programId);

        if (JWSUtil.isMultipartRequest(httpServletRequest)) {
            log.info("This is file based request");
            String localFileName = fileStorageService.save(JWSUtil.parseFormData(httpServletRequest), fileName);
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
            Headers headers = headerBuilder.addHeader(FILE_NAME, localFileName).build();

            CamelApiResponse response = sendRequestToCamel(headers);
            httpServletResponse.setStatus(response.getStatus());
            return response.getBody();
        }
    }

    @ExceptionHandler({ MultipartException.class })
    public String handleMultipartException(HttpServletResponse httpServletResponse) {
        JSONObject json = new JSONObject();
        json.put("Error Information: ", "File not uploaded");
        json.put("Error Description : ", "There was no fie uploaded with the request. " + "Please upload a file and try again.");
        httpServletResponse.setStatus(httpServletResponse.SC_BAD_REQUEST);
        return json.toString();
    }

    private CamelApiResponse sendRequestToCamel(Headers headers) {
        Exchange exchange = SpringWrapperUtil.getDefaultWrappedExchange(producerTemplate.getCamelContext(), headers);
        exchange = producerTemplate.send("direct:post-batch-transactions", exchange);
        int statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String body = exchange.getIn().getBody(String.class);
        return new CamelApiResponse(body, statusCode);
    }

}
