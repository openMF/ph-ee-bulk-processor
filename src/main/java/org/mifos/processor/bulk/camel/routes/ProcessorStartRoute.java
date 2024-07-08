package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.BATCH_REQUEST_TYPE;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PLATFORM_TENANT_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.IS_UPDATED;
import static org.mifos.processor.bulk.camel.config.CamelProperties.LOCAL_FILE_PATH;
import static org.mifos.processor.bulk.camel.config.CamelProperties.OVERRIDE_HEADER;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TENANT_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.APPROVAL_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.AUTHORIZATION_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_AGGREGATE_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BULK_NOTIF_FAILURE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BULK_NOTIF_SUCCESS;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_THRESHOLD;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_THRESHOLD_CHECK_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.DE_DUPLICATION_ENABLE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FORMATTING_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_CALLBACK_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_STATUS_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MERGE_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ORDERING_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_LOOKUP_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYEE_DFSP_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.SPLITTING_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.THRESHOLD_DELAY;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.tika.Tika;
import org.json.JSONObject;
import org.mifos.processor.bulk.camel.config.CamelProperties;
import org.mifos.processor.bulk.config.BudgetAccountConfig;
import org.mifos.processor.bulk.connectors.service.ProcessorStartRouteService;
import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.properties.TenantImplementation;
import org.mifos.processor.bulk.properties.TenantImplementationProperties;
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
    TenantImplementationProperties tenantImplementationProperties;

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

    @Autowired
    BudgetAccountConfig budgetAccountConfig;

    @Override
    public void configure() {
        setup();
    }

    private void setup() {

        from("direct:post-batch-transactions").id("rest:POST:/batchtransactions").log("Starting route rest:POST:/batchtransactions")
                .to("direct:validate-file").choice().when(header("CamelHttpResponseCode").isNotEqualTo("200"))
                .log(LoggingLevel.ERROR, "File upload failed").otherwise().process(exchange -> {
                    String batchId = UUID.randomUUID().toString();
                    exchange.setProperty(BATCH_ID, batchId);

                }).bean(ProcessorStartRouteService.class, "validateFileSyncResponse").choice()
                .when(header("CamelHttpResponseCode").isNotEqualTo("200")).log(LoggingLevel.ERROR, "File upload failed").otherwise()
                .to("direct:executeBatch").endChoice().endChoice();

        from("direct:post-bulk-transfer").unmarshal().mimeMultipart("multipart/*").to("direct:validate-tenant").process(exchange -> {
            String fileName = System.currentTimeMillis() + "_" + exchange.getIn().getHeader("fileName", String.class);
            String requestId = exchange.getIn().getHeader("requestId", String.class);
            String purpose = exchange.getIn().getHeader("purpose", String.class);
            String batchId = UUID.randomUUID().toString();
            String payeeDfspId = exchange.getIn().getHeader(CamelProperties.PAYEE_DFSP_ID, String.class);
            String callbackUrl = exchange.getIn().getHeader("X-CallbackURL", String.class);
            exchange.setProperty(CALLBACK, callbackUrl);
            exchange.setProperty(BATCH_ID, batchId);
            exchange.setProperty(FILE_NAME, fileName);
            exchange.setProperty(REQUEST_ID, requestId);
            exchange.setProperty(PURPOSE, purpose);
            exchange.setProperty(PAYEE_DFSP_ID, payeeDfspId);
        }).wireTap("direct:start-batch-process-csv");

        from("direct:validate-tenant").id("direct:validate-tenant").log("Validating tenant").process(exchange -> {
            String tenantName = exchange.getIn().getHeader(HEADER_PLATFORM_TENANT_ID, String.class);
            // validation is disabled for now
            /*
             * if (tenantName == null || tenantName.isEmpty() || !tenants.contains(tenantName)) { throw new
             * Exception("Invalid tenant value."); }
             */
            exchange.setProperty(TENANT_NAME, tenantName);
        }).setHeader("Content-Type", constant("application/json;charset=UTF-8")).log("Completed route direct:validate-tenant");

        // this route is responsible for editing the incoming records based on configuration
        // this step is done to make sure the file format of CSV is not altered and only the data is updated based on
        // config
        from("direct:update-incoming-data").id("direct:update-incoming-data").log("direct:update-incoming-data")
                // [LOCAL_FILE_PATH] is already set in [direct:validateFileSyncResponse] route
                .setProperty(LOCAL_FILE_PATH, exchangeProperty(FILE_NAME)).to("direct:get-transaction-array")
                // make sure new data is set under the exchange variable [RESULT_TRANSACTION_LIST]
                .bean(ProcessorStartRouteService.class, "updateIncomingData").choice()
                // update only when previous(edit function) makes any changes to data
                .when(exchange -> exchange.getProperty(IS_UPDATED, Boolean.class))
                // warning: changing this flag can break things
                .setProperty(OVERRIDE_HEADER, constant(true)) // default header in CSV file will be used
                .to("direct:update-file-v2").otherwise().log(LoggingLevel.INFO, "No update");

        from("direct:start-batch-process-csv").id("direct:start-batch-process-csv").log("Starting route direct:start-batch-process-csv")
                .to("direct:update-incoming-data").bean(ProcessorStartRouteService.class, "startBatchProcessCsv")
                .log("Completed route direct:start-batch-process-csv").bean(ProcessorStartRouteService.class, "pollingOutput");

        from("direct:start-batch-process-raw").id("direct:start-batch-process-raw").log("Starting route direct:start-batch-process-raw")
                .process(exchange -> {
                    JSONObject response = new JSONObject();
                    response.put("batch_id", UUID.randomUUID().toString());
                    response.put("request_id", UUID.randomUUID().toString());
                    response.put("status", "queued");
                    exchange.getIn().setBody(response.toString());
                }).log("Completed route direct:start-batch-process-raw");

        from("direct:executeBatch").id("direct:executeBatch").log("Starting route direct:executeBatch")
                .bean(ProcessorStartRouteService.class, "validateTenant").bean(ProcessorStartRouteService.class, "executeBatch").choice()
                .when(exchange -> exchange.getProperty(BATCH_REQUEST_TYPE, String.class).equalsIgnoreCase("raw"))
                .bean(ProcessorStartRouteService.class, "startBatchProcessRaw")
                .when(exchange -> exchange.getProperty(BATCH_REQUEST_TYPE, String.class).equalsIgnoreCase("csv"))
                .to("direct:start-batch-process-csv").otherwise()
                .setBody(exchange -> getUnsupportedTypeJson(exchange.getProperty(BATCH_REQUEST_TYPE, String.class)).toString())
                .log("Completed execution of route rest:POST:/batchtransactions");

        from("direct:pollingOutput").id("direct:pollingOutput").log("Started pollingOutput route").process(exchange -> {
            JSONObject json = new JSONObject();
            json.put("PollingPath", "/batch/Summary/" + exchange.getProperty(BATCH_ID));
            json.put("SuggestedCallbackSeconds", pollApiTimer);
            exchange.getIn().setBody(json.toString());
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 202);
        });

        from("direct:validateFileSyncResponse").id("direct:validateFileSyncResponse").log("Starting route direct:validateFileSyncResponse")
                .process(exchange -> {
                    // move this logic to spring
                    String fileName = exchange.getIn().getHeader(FILE_NAME, String.class);
                    File file = new File(fileName);

                    // check the file structure
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
            File f = new File(exchange.getIn().getHeader(FILE_NAME, String.class));
            logger.debug("File name: {} ", f.getName());
            Tika tika = new Tika();
            String fileType = tika.detect(f.getName());
            logger.debug("File type: {} ", fileType);
            if (f.getName().isEmpty()) {
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

    public boolean verifyData(File file) throws IOException {
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

    public String getWorkflowForTenant(String tenantId, String useCase) {

        for (TenantImplementation tenant : tenantImplementationProperties.getTenants()) {
            if (tenant.getId().equals(tenantId)) {
                return tenant.getFlows().getOrDefault(useCase, "default");
            }
        }
        return "default";
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

    public boolean verifyCsv(File csvData) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(csvData));
        String header = br.readLine();
        String[] columns = new String[0];
        if (header != null) {
            columns = header.split(",");
            logger.debug("Columns in the csv file are {}", Arrays.toString(columns));
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

    public void setErrorResponse(Exchange exchange, int responseCode, String errorInfo, String errorDescription) {
        // TODO Auto-generated method stub
        JSONObject json = new JSONObject();
        json.put("Error Information: ", errorInfo);
        json.put("Error Description : ", errorDescription);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
        exchange.getIn().setBody(json.toString());
        exchange.setProperty("body", json);
        logger.error("Error response is {}", json);
    }

    public void setResponse(Exchange exchange, int responseCode) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, responseCode);
    }

    public Map<String, Object> setConfigProperties(Map<String, Object> variables) {
        variables.put(BATCH_AGGREGATE_ENABLED, workerConfig.isBatchAggregateEnabled);
        variables.put(PARTY_LOOKUP_ENABLED, workerConfig.isPartyLookUpWorkerEnabled);
        variables.put(AUTHORIZATION_ENABLED, workerConfig.isAuthorizationWorkerEnabled);
        variables.put(APPROVAL_ENABLED, workerConfig.isApprovalWorkerEnabled);
        variables.put(DE_DUPLICATION_ENABLE, workerConfig.isTransactionDeduplicationEnabled);
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
