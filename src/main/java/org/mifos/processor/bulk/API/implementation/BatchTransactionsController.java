package org.mifos.processor.bulk.api.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.json.JSONObject;
import org.mifos.processor.bulk.api.definition.BatchTransactions;
import org.mifos.processor.bulk.file.FileStorageService;
import org.mifos.processor.bulk.utility.Headers;
import org.mifos.processor.bulk.utility.SpringWrapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;

@Slf4j
@RestController
public class BatchTransactionsController implements BatchTransactions {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    FileStorageService fileStorageService;

    @SneakyThrows
    @Override
    public String batchTransactions(HttpServletResponse httpServletResponse,
                                    String requestId, MultipartFile file, String fileName,
                                    String purpose, String type, String tenant,
                                    String registeringInstitutionId, String programId) throws IOException {
        log.debug("Inside api logic");
        String localFileName = fileStorageService.save(file);
        Headers headers = new Headers.HeaderBuilder()
                .addHeader(HEADER_CLIENT_CORRELATION_ID, requestId)
                .addHeader(PURPOSE,purpose)
                .addHeader(FILE_NAME,localFileName)
                .addHeader("Type",type)
                .addHeader(HEADER_PLATFORM_TENANT_ID,tenant)
                .addHeader(HEADER_REGISTERING_INSTITUTE_ID, registeringInstitutionId)
                .addHeader(HEADER_PROGRAM_ID, programId)
                .build();
		log.debug("Headers passed: {}", headers);
        Exchange exchange = SpringWrapperUtil.getDefaultWrappedExchange(producerTemplate.getCamelContext(),
              headers);
        log.debug("Header in exchange: {}", exchange.getIn().getHeaders());
        exchange = producerTemplate.send("direct:post-batch-transactions", exchange);
        int statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
		httpServletResponse.setStatus(statusCode);
       	return exchange.getIn().getBody(String.class);
    }

    @ExceptionHandler({MultipartException.class})
    public String handleMultipartException(HttpServletResponse httpServletResponse) {
        JSONObject json = new JSONObject();
        json.put("Error Information: ", "File not uploaded");
        json.put("Error Description : ", "There was no fie uploaded with the request. " +
                "Please upload a file and try again.");
        httpServletResponse.setStatus(httpServletResponse.SC_BAD_REQUEST);
        return json.toString();
    }

}
