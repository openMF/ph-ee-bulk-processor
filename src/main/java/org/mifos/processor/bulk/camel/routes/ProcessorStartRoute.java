package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.BATCH_REQUEST_TYPE;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TENANT_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.APPROVAL_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BULK_NOTIF_FAILURE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BULK_NOTIF_SUCCESS;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_URL;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_THRESHOLD;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_THRESHOLD_CHECK_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_VALIDITY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FORMATTING_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_CALLBACK_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_STATUS_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MERGE_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.NOTE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ORDERING_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_LOOKUP_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASES;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASE_COUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.SPLITTING_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.THRESHOLD_DELAY;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.activation.DataHandler;
import javax.mail.internet.MimeBodyPart;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.tika.Tika;
import org.json.JSONObject;
import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.utility.PhaseUtils;
import org.mifos.processor.bulk.utility.Utils;
import org.mifos.processor.bulk.zeebe.ZeebeProcessStarter;
import org.mifos.processor.bulk.zeebe.worker.WorkerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    @Value("${config.completion-threshold-check.completion-threshold}")
    private int completionThreshold;

    @Value("${config.completion-threshold-check.max-retry}")
    private int maxThresholdCheckRetry;

    @Value("${config.completion-threshold-check.delay}")
    private int thresholdCheckDelay;

    @Value("${callback.max-retry}")
    private int maxCallbackRetry;

    @Value("${pollingApi.timer}")
    private String pollApiTimer;

    @Value("#{'${csv.columnNames}'.split(',')}")
    private List<String> columnNames;

    @Value("${csv.size}")
    private int csvSize;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    PhaseUtils phaseUtils;

    @Override
    public void configure() {
        setup();
    }

    private void setup() {

        from("rest:POST:/batchtransactions").id("rest:POST:/batchtransactions").log("Starting route rest:POST:/batchtransactions")
                .to("direct:validate-file").choice().when(header("CamelHttpResponseCode").isNotEqualTo("200"))
                .log(LoggingLevel.ERROR, "File upload failed").otherwise().unmarshal().mimeMultipart("multipart/*").process(exchange -> {
                    String batchId = UUID.randomUUID().toString();
                    exchange.setProperty(BATCH_ID, batchId);

                }).unmarshal().mimeMultipart("multipart/*").to("direct:validateFileSyncResponse").choice()
                .when(header("CamelHttpResponseCode").isNotEqualTo("200")).log(LoggingLevel.ERROR, "File upload failed").otherwise()
                .unmarshal().mimeMultipart("multipart/*").wireTap("direct:executeBatch").to("direct:pollingOutput").endChoice().endChoice();

        from("rest:POST:/bulk/transfer/{requestId}/{fileName}").unmarshal().mimeMultipart("multipart/*").to("direct:validate-tenant")
                .process(exchange -> {
                    String fileName = System.currentTimeMillis() + "_" + exchange.getIn().getHeader("fileName", String.class);
                    String requestId = exchange.getIn().getHeader("requestId", String.class);
                    String purpose = exchange.getIn().getHeader("purpose", String.class);
                    String batchId = UUID.randomUUID().toString();
                    exchange.setProperty(BATCH_ID, batchId);
                    exchange.setProperty(FILE_NAME, fileName);
                    exchange.setProperty(REQUEST_ID, requestId);
                    exchange.setProperty(PURPOSE, purpose);
                }).wireTap("direct:start-batch-process-csv").to("direct:pollingOutput");

        from("direct:validate-tenant").id("direct:validate-tenant").log("Validating tenant").process(exchange -> {
            String tenantName = exchange.getIn().getHeader("Platform-TenantId", String.class);
            // validation is disabled for now
            /*
             * if (tenantName == null || tenantName.isEmpty() || !tenants.contains(tenantName)) { throw new
             * Exception("Invalid tenant value."); }
             */
            exchange.setProperty(TENANT_NAME, tenantName);
        }).setHeader("Content-Type", constant("application/json;charset=UTF-8")).log("Completed route direct:validate-tenant");

        from("direct:start-batch-process-csv").id("direct:start-batch-process-csv").log("Starting route direct:start-batch-process-csv")
                .process(exchange -> {
                    String fileName = exchange.getProperty(FILE_NAME, String.class);
                    String requestId = exchange.getProperty(REQUEST_ID, String.class);
                    String purpose = exchange.getProperty(PURPOSE, String.class);
                    String batchId = exchange.getProperty(BATCH_ID, String.class);
                    String note = null;

                    if (purpose == null || purpose.isEmpty()) {
                        purpose = "test payment";
                    }

                    logger.info("\n\n Filename: {}", fileName);
                    logger.info("\n\n BatchId: {} ", batchId);

                    InputStream csvData = exchange.getIn().getBody(InputStream.class);
                    File file = setupFile(csvData, fileName);
                    logger.debug("File length {}", file.length());
                    boolean verifyData = verifyData(file);
                    logger.debug("Data verification result {}", verifyData);
                    if (!verifyData) {
                        note = "Invalid data in file data processing stopped";

                    }

                    String nm = fileTransferService.uploadFile(file, bucketName);

                    logger.info("File uploaded {}", nm);

                    // extracting and setting callback Url
                    String callbackUrl = exchange.getIn().getHeader("X-Callback-URL", String.class);
                    exchange.setProperty(CALLBACK_URL, callbackUrl);

                    List<Integer> phases = phaseUtils.getValues();
                    logger.debug(phases.toString());
                    Map<String, Object> variables = new HashMap<>();
                    variables.put(FILE_VALIDITY, verifyData);
                    variables.put(BATCH_ID, batchId);
                    variables.put(FILE_NAME, fileName);
                    variables.put(REQUEST_ID, requestId);
                    variables.put(PURPOSE, purpose);
                    variables.put(TENANT_ID, exchange.getProperty(TENANT_NAME));
                    variables.put(CALLBACK_URL, callbackUrl);
                    variables.put(PHASES, phases);
                    variables.put(PHASE_COUNT, phases.size());
                    variables.put(NOTE, note);
                    setConfigProperties(variables);

                    JSONObject response = new JSONObject();

                    try {
                        String tenantSpecificWorkflowId = workflowId.replace("{dfspid}", exchange.getProperty(TENANT_NAME).toString());
                        String txnId = zeebeProcessStarter.startZeebeWorkflow(tenantSpecificWorkflowId, "", variables);
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

                }).log("Completed route direct:start-batch-process-csv");

        from("direct:start-batch-process-raw").id("direct:start-batch-process-raw").log("Starting route direct:start-batch-process-raw")
                .process(exchange -> {
                    JSONObject response = new JSONObject();
                    response.put("batch_id", UUID.randomUUID().toString());
                    response.put("request_id", UUID.randomUUID().toString());
                    response.put("status", "queued");
                    exchange.getIn().setBody(response.toString());
                }).log("Completed route direct:start-batch-process-raw");

        from("direct:executeBatch").id("direct:executeBatch").log("Starting route direct:executeBatch").to("direct:validate-tenant")
                .process(exchange -> {
                    String filename = exchange.getIn().getHeader("filename", String.class);
                    String requestId = exchange.getIn().getHeader("X-CorrelationID", String.class);
                    String purpose = exchange.getIn().getHeader("Purpose", String.class);
                    String type = exchange.getIn().getHeader("Type", String.class);
                    exchange.setProperty(FILE_NAME, filename);
                    exchange.setProperty(REQUEST_ID, requestId);
                    exchange.setProperty(PURPOSE, purpose);
                    exchange.setProperty(BATCH_REQUEST_TYPE, type);
                }).choice().when(exchange -> exchange.getProperty(BATCH_REQUEST_TYPE, String.class).equalsIgnoreCase("raw"))
                .to("direct:start-batch-process-raw")
                .when(exchange -> exchange.getProperty(BATCH_REQUEST_TYPE, String.class).equalsIgnoreCase("csv")).unmarshal()
                .mimeMultipart("multipart/*").to("direct:start-batch-process-csv").otherwise()
                .setBody(exchange -> getUnsupportedTypeJson(exchange.getProperty(BATCH_REQUEST_TYPE, String.class)).toString())
                .log("Completed execution of route rest:POST:/batchtransactions");

        from("direct:pollingOutput").id("direct:pollingOutput").log("Started pollingOutput route").process(exchange -> {
            JSONObject json = new JSONObject();
            json.put("PollingPath", "/batch/Summary/" + exchange.getProperty(BATCH_ID));
            json.put("SuggestedCallbackSeconds", pollApiTimer);
            exchange.getIn().setBody(json.toString());

        });

        from("direct:validateFileSyncResponse").id("direct:validateFileSyncResponse").log("Starting route direct:validateFileSyncResponse")
                .process(exchange -> {
                    String fileName = System.currentTimeMillis() + "_" + exchange.getIn().getHeader("fileName", String.class);
                    InputStream csvData = exchange.getIn().getBody(InputStream.class);
                    File file = setupFile(csvData, fileName);
                    int fileSize = (int) file.length();
                    if (fileSize > csvSize) {
                        setErrorResponse(exchange, 400, "File too big",
                                "The file uploaded is too big. " + "Please upload a file and try again.");
                    } else if (!verifyCsv(file)) {
                        setErrorResponse(exchange, 400, "Invalid file structure",
                                "The file uploaded contains wrong structure." + " Please upload correct file columns and try again.");
                    } else {
                        logger.debug("Filename: {}", fileName);
                        setResponse(exchange, 200);
                    }

                }).log("Completed route direct:validateFileSyncResponse");

        from("direct:validate-file").id("direct:validate-file").log("Starting route direct:validate-file").process(exchange -> {
            System.setProperty("mail.mime.multipart.ignoreexistingboundaryparameter", "true");
            System.setProperty("mail.mime.multipart.allowempty", "true");
            InputStream inputStreams = exchange.getIn().getBody(InputStream.class);
            MimeBodyPart mimeMessage = new MimeBodyPart(inputStreams);
            DataHandler dh = mimeMessage.getDataHandler();
            logger.debug("File name: {} ", dh.getName());
            Tika tika = new Tika();
            String fileType = tika.detect(dh.getName());
            logger.debug("File type: {} ", fileType);
            if (dh.getName() == null || dh.getName().isEmpty()) {
                setErrorResponse(exchange, 400, "File not uploaded",
                        "There was no fie uploaded with the request. " + "Please upload a file and try again.");
            } else if (!fileType.equalsIgnoreCase("text/csv")) {
                setErrorResponse(exchange, 400, "Broken file",
                        "The file uploaded is broken as it has a different extension. " + "Please upload a csv file and try again.");
            } else {
                setResponse(exchange, 200);
            }

        });
    }

    private boolean verifyData(File file) throws IOException {
        InputStream ips = new FileInputStream(file);
        InputStreamReader ipsr = new InputStreamReader(ips);
        BufferedReader br = new BufferedReader(ipsr);
        String line;
        br.readLine();
        while ((line = br.readLine()) != null) {
            String[] row = line.split(",");
            if (row.length != columnNames.size()) {
                logger.debug("Row invalid {} {}", row.length, columnNames.size());
                return false;
            }
            if (!verifyRow(row)) {
                return false;
            }
        }
        return true;
    }

    private boolean verifyRow(String[] row) {
        for (int i = 1; i < row.length; i++) {
            row[i] = row[i].trim();
            if (row[i].equalsIgnoreCase("MSISDN")) {
                int j = row[i].indexOf("MSISDN");
                if (!(j == row.length)) {
                    if (!row[j + 1].matches("^[0-9]*$")) {
                        logger.debug("MSISDN invalid");
                        return false;
                    }
                }
            } else if (row[i].contains("amount")) {
                int j = row[i].indexOf("amount");
                if (!row[j].matches("^[0-9]*$")) {
                    logger.debug("Amount invalid");
                    return false;
                }

            }
        }
        return true;
    }

    private File setupFile(InputStream csvStream, String fileName) throws IOException {
        String csvData = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"));
        File file = new File(fileName);
        file.setWritable(true);
        file.setReadable(true);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(csvData);
        fileWriter.close();
        return file;
    }

    private boolean verifyCsv(File csvData) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(csvData));
        String header = br.readLine();
        String[] columns = new String[0];
        if (header != null) {
            columns = header.split(",");
            logger.info("Columns in the csv file are {}", Arrays.toString(columns));
        }
        int i = 0;
        while (i < columns.length) {
            if (columnNames.contains(columns[i])) {
                logger.debug("Column name {} is at index {} ", columns[i], columnNames.indexOf(columns[i]));
                i++;

            } else {
                return false;
            }
        }
        return true;
    }

    private void setErrorResponse(Exchange exchange, int responseCode, String errorInfo, String errorDescription) {
        // TODO Auto-generated method stub
        JSONObject json = new JSONObject();
        json.put("Error Information: ", errorInfo);
        json.put("Error Description : ", errorDescription);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
        exchange.getIn().setBody(json.toString());
        exchange.setProperty("body", json);
        logger.error("Error response is {}", json);
    }

    private void setResponse(Exchange exchange, int responseCode) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);

    }

    private Map<String, Object> setConfigProperties(Map<String, Object> variables) {
        variables.put(PARTY_LOOKUP_ENABLED, workerConfig.isPartyLookUpWorkerEnabled);
        variables.put(APPROVAL_ENABLED, workerConfig.isApprovalWorkerEnabled);
        variables.put(ORDERING_ENABLED, workerConfig.isOrderingWorkerEnabled);
        variables.put(SPLITTING_ENABLED, workerConfig.isSplittingWorkerEnabled);
        variables.put(FORMATTING_ENABLED, workerConfig.isFormattingWorkerEnabled);
        variables.put(COMPLETION_THRESHOLD_CHECK_ENABLED, workerConfig.isCompletionThresholdCheckEnabled);
        variables.put(MERGE_ENABLED, workerConfig.isMergeBackWorkerEnabled);
        variables.put(MAX_STATUS_RETRY, maxThresholdCheckRetry);
        variables.put(COMPLETION_THRESHOLD, completionThreshold);
        variables.put(THRESHOLD_DELAY, Utils.getZeebeTimerValue(thresholdCheckDelay));
        variables.put(BULK_NOTIF_SUCCESS, false);
        variables.put(BULK_NOTIF_FAILURE, false);
        variables.put(MAX_CALLBACK_RETRY, maxCallbackRetry);

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
