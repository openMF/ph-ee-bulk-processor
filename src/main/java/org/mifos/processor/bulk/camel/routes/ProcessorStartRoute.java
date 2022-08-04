package org.mifos.processor.bulk.camel.routes;

import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.zeebe.ZeebeProcessStarter;
import org.mifos.processor.bulk.zeebe.worker.WorkerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;


@Component
public class ProcessorStartRoute extends BaseRouteBuilder {

    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;

    @Autowired
    protected WorkerConfig workerConfig;

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
                    variables.put(FILE_NAME, fileName);
                    variables.put(REQUEST_ID, requestId);
                    variables.put(PARTY_LOOKUP_ENABLED, workerConfig.isPartyLookUpWorkerEnabled);
                    variables.put(APPROVAL_ENABLED, workerConfig.isApprovalWorkerEnabled);
                    variables.put(ORDERING_ENABLED, workerConfig.isOrderingWorkerEnabled);
                    variables.put(SPLITTING_ENABLED, workerConfig.isSplittingWorkerEnabled);
                    variables.put(FORMATTING_ENABLED, workerConfig.isFormattingWorkerEnabled);
                    variables.put(SUCCESS_THRESHOLD_CHECK_ENABLED, workerConfig.isSuccessThresholdCheckEnabled);
                    variables.put(MERGE_ENABLED, workerConfig.isMergeBackWorkerEnabled);

                    zeebeProcessStarter.startZeebeWorkflow(workflowId, "", variables);
                });
    }
}
