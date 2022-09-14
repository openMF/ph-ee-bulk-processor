package org.mifos.processor.bulk.camel.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.json.JSONObject;
import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.utility.Utils;
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

import static org.mifos.processor.bulk.camel.config.CamelProperties.BATCH_REQUEST_TYPE;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TENANT_NAME;
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

    @Value("${config.success-threshold-check.success-threshold}")
    private int successThreshold;

    @Value("${config.success-threshold-check.max-retry}")
    private int maxThresholdCheckRetry;

    @Value("${config.success-threshold-check.delay}")
    private int thresholdCheckDelay;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void configure() {
        setup();
    }

    private void setup() {

        from("rest:POST:/batchtransactions")
                .id("rest:POST:/batchtransactions")
                .log("Starting route rest:POST:/batchtransactions")
                .to("direct:validate-tenant")
                .process(exchange -> {
                    String filename = exchange.getIn().getHeader("filename", String.class);
                    String requestId = exchange.getIn().getHeader("X-CorrelationID", String.class);
                    String purpose = exchange.getIn().getHeader("Purpose", String.class);
                    String type = exchange.getIn().getHeader("Type", String.class);
                    exchange.setProperty(FILE_NAME, filename);
                    exchange.setProperty(REQUEST_ID, requestId);
                    exchange.setProperty(PURPOSE, purpose);
                    exchange.setProperty(BATCH_REQUEST_TYPE, type);
                })
                .choice()
                .when(exchange -> exchange.getProperty(BATCH_REQUEST_TYPE, String.class).equalsIgnoreCase("raw"))
                .to("direct:start-batch-process-raw")
                .when(exchange -> exchange.getProperty(BATCH_REQUEST_TYPE, String.class).equalsIgnoreCase("csv"))
                .unmarshal().mimeMultipart("multipart/*")
                .to("direct:start-batch-process-csv")
                .otherwise()
                .setBody(exchange -> getUnsupportedTypeJson(exchange.getProperty(BATCH_REQUEST_TYPE, String.class)).toString())
                .log("Completed execution of route rest:POST:/batchtransactions");


        from("rest:POST:/bulk/transfer/{requestId}/{fileName}")
                .unmarshal().mimeMultipart("multipart/*")
                .to("direct:validate-tenant")
                .process(exchange -> {
                    String fileName = System.currentTimeMillis() + "_" +  exchange.getIn().getHeader("fileName", String.class);
                    String requestId = exchange.getIn().getHeader("requestId", String.class);
                    String purpose = exchange.getIn().getHeader("purpose", String.class);

                    exchange.setProperty(FILE_NAME, fileName);
                    exchange.setProperty(REQUEST_ID, requestId);
                    exchange.setProperty(PURPOSE, purpose);
                })
                .to("direct:start-batch-process-csv");

        from("direct:validate-tenant")
                .id("direct:validate-tenant")
                .log("Validating tenant")
                .process(exchange -> {
                    String tenantName = exchange.getIn().getHeader("Platform-TenantId", String.class);
                    if (tenantName == null || tenantName.isEmpty() || !tenants.contains(tenantName)) {
                        throw new Exception("Invalid tenant value.");
                    }
                    exchange.setProperty(TENANT_NAME, tenantName);
                })
                .setHeader("Content-Type", constant("application/json;charset=UTF-8"))
                .log("Completed route direct:validate-tenant");

        from("direct:start-batch-process-csv")
                .id("direct:start-batch-process-csv")
                .log("Starting route direct:start-batch-process-csv")
                .process(exchange -> {
                    String fileName = exchange.getProperty(FILE_NAME, String.class);
                    String requestId = exchange.getProperty(REQUEST_ID, String.class);
                    String purpose = exchange.getProperty(PURPOSE, String.class);
                    String batchId = UUID.randomUUID().toString();


                    if (purpose == null || purpose.isEmpty()) {
                        purpose = "test payment";
                    }

                    logger.info("\n\n Filename: " + fileName + " \n\n");
                    logger.info("\n\n BatchId: " + batchId + " \n\n");

                    File file = new File(fileName);
                    file.setWritable(true);
                    file.setReadable(true);

                    String csvData = exchange.getIn().getBody(String.class);
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(csvData);
                    fileWriter.close();

                    logger.info(""+file.length());
                    logger.info(file.getAbsolutePath());

                    String nm = fileTransferService.uploadFile(file, bucketName);

                    logger.info("File uploaded {}", nm);


                    Map<String, Object> variables = new HashMap<>();
                    variables.put(BATCH_ID, batchId);
                    variables.put(FILE_NAME, fileName);
                    variables.put(REQUEST_ID, requestId);
                    variables.put(PURPOSE, purpose);
                    variables.put(TENANT_ID, exchange.getProperty(TENANT_NAME));
                    setConfigProperties(variables);

                    JSONObject response = new JSONObject();

                    try {
                        String txnId = zeebeProcessStarter.startZeebeWorkflow(workflowId, "", variables);
                        if (txnId == null || txnId.isEmpty()) {
                            response.put("errorCode", 500);
                            response.put("errorDescription", "Unable to start zeebe workflow");
                            response.put("developerMessage", "Issue in starting the zeebe workflow, check the zeebe configuration");
                        } else {
                            response.put("batch_id", batchId);
                            response.put("request_id", requestId);
                            response.put("status", "queued");
                        }
                    } catch (Exception e) {
                        response.put("errorCode", 500);
                        response.put("errorDescription", "Unable to start zeebe workflow");
                        response.put("developerMessage", e.getLocalizedMessage());
                    }

                    exchange.getIn().setBody(response.toString());

                })
                .log("Completed route direct:start-batch-process-csv");


        from("direct:start-batch-process-raw")
                .id("direct:start-batch-process-raw")
                .log("Starting route direct:start-batch-process-raw")
                .process(exchange -> {
                    JSONObject response = new JSONObject();
                    response.put("batch_id", UUID.randomUUID().toString());
                    response.put("request_id", UUID.randomUUID().toString());
                    response.put("status", "queued");
                    exchange.getIn().setBody(response.toString());
                })
                .log("Completed route direct:start-batch-process-raw");
    }

    private Map<String, Object> setConfigProperties(Map<String, Object> variables) {
        variables.put(PARTY_LOOKUP_ENABLED, workerConfig.isPartyLookUpWorkerEnabled);
        variables.put(APPROVAL_ENABLED, workerConfig.isApprovalWorkerEnabled);
        variables.put(ORDERING_ENABLED, workerConfig.isOrderingWorkerEnabled);
        variables.put(SPLITTING_ENABLED, workerConfig.isSplittingWorkerEnabled);
        variables.put(FORMATTING_ENABLED, workerConfig.isFormattingWorkerEnabled);
        variables.put(SUCCESS_THRESHOLD_CHECK_ENABLED, workerConfig.isSuccessThresholdCheckEnabled);
        variables.put(MERGE_ENABLED, workerConfig.isMergeBackWorkerEnabled);
        variables.put(MAX_STATUS_RETRY, maxThresholdCheckRetry);
        variables.put(SUCCESS_THRESHOLD, successThreshold);
        variables.put(THRESHOLD_DELAY, Utils.getZeebeTimerValue(thresholdCheckDelay));
        return variables;
    }

    private JSONObject getUnsupportedTypeJson(String type) {
        JSONObject response = new JSONObject();
        response.put("errorCode", 400);
        response.put("errorDescription", String.format("Query parameter ?type=%s not supported", type));
        response.put("developerMessage", "");
        return response;
    }
}
