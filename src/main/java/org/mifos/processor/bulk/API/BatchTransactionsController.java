package org.mifos.processor.bulk.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.mifos.processor.bulk.utility.Headers;
import org.mifos.processor.bulk.utility.SpringWrapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@RestController
public class BatchTransactionsController implements BatchTransactions {
    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public String batchTransactions(String requestId, MultipartFile file, String fileName, String purpose, String type, String tenant) throws IOException {
        assert (file.getSize() >0);
        Headers headers = new Headers.HeaderBuilder()
                .addHeader("X-CorrelationID", requestId)
                .addHeader(PURPOSE,purpose)
                .addHeader(FILE_NAME,fileName)
                .addHeader("Type",type)
                .addHeader("Platform-TenantId",tenant)
                .build();
        Exchange exchange = SpringWrapperUtil.getDefaultWrappedExchange(producerTemplate.getCamelContext(),
              headers, new String(file.getBytes()));
        System.out.println("Requested file = "  + new String(file.getBytes())  );
        producerTemplate.send("direct:post-batch-transactions", exchange);
       return exchange.getIn().getBody(String.class);
    }
}
