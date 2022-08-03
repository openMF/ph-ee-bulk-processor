package org.mifos.processor.bulk.camel.routes;

import io.undertow.util.MultipartParser;
import org.apache.camel.attachment.Attachment;
import org.apache.camel.attachment.AttachmentMessage;
import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.zeebe.ZeebeProcessStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;

@Component
public class ProcessorStartRoute extends BaseRouteBuilder{

    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;

    @Value("${application.bucket-name}")
    private String bucketName;

    @Value("${bpmn.flows.bulk-processor}")
    private String workflowId;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void configure() throws Exception {
        setup();
    }

    private void setup() {
        from("rest:POST:/bulk/transfer/{requestId}/{fileName}")
                .unmarshal().mimeMultipart("multipart/*")
                .process(exchange -> {
                    String fileName = exchange.getIn().getHeader("fileName", String.class);
                    String requestId = exchange.getIn().getHeader("requestId", String.class);
                    String batchId = UUID.randomUUID().toString();

                    logger.info("\n\n Filename: " + fileName + " \n\n");
                    logger.info("\n\n BatchId: " + batchId + " \n\n");

                    File file = new File(fileName);
                    file.setWritable(true);
                    file.setReadable(true);

                    String csvData = exchange.getIn().getBody(String.class);
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(csvData);
                    fileWriter.close();

                    logger.info(csvData);
                    logger.info(""+file.length());
                    logger.info(file.getAbsolutePath());

                    String nm = fileTransferService.uploadFile(file, bucketName);

                    logger.info("File uploaded {}", nm);

                    
                    Map<String, Object> variables = new HashMap<>();
                    variables.put(BATCH_ID, batchId);
                    variables.put("fileName", fileName);
                    variables.put("requestId", requestId);

                    zeebeProcessStarter.startZeebeWorkflow(workflowId, "", variables);
                });
    }
}
