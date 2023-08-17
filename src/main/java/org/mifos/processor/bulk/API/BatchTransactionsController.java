package org.mifos.processor.bulk.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.tika.Tika;
import org.json.JSONObject;
import org.mifos.processor.bulk.file.FileStorageService;
import org.mifos.processor.bulk.utility.Headers;
import org.mifos.processor.bulk.utility.SpringWrapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;
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
                                    String purpose, String type, String tenant) throws IOException {
        assert (file.getSize() >0);
        log.info("Inside api logic");
        String localFileName = fileStorageService.save(file);
        Headers headers = new Headers.HeaderBuilder()
                .addHeader("X-CorrelationID", requestId)
                .addHeader(PURPOSE,purpose)
                .addHeader(FILE_NAME,localFileName)
                .addHeader("Type",type)
                .addHeader("Platform-TenantId",tenant)
                .build();

        Exchange exchange = SpringWrapperUtil.getDefaultWrappedExchange(producerTemplate.getCamelContext(),
              headers);
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
//        httpServletResponse.getWriter().write(json.toString());
//        httpServletResponse.getWriter().flush();

        return json.toString();
    }

}
