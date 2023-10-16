package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PLATFORM_TENANT_ID;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.tika.Tika;
import org.json.JSONObject;
import org.mifos.processor.bulk.config.BudgetAccountConfig;
import org.mifos.processor.bulk.config.Program;
import org.mifos.processor.bulk.config.RegisteringInstitutionConfig;
import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.schema.Transaction;
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
import org.springframework.util.StringUtils;

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

                }).to("direct:validateFileSyncResponse").choice().when(header("CamelHttpResponseCode").isNotEqualTo("200"))
                .log(LoggingLevel.ERROR, "File upload failed").otherwise().wireTap("direct:executeBatch").to("direct:pollingOutput")
                .endChoice().endChoice();

        from("direct:post-bulk-transfer").unmarshal().mimeMultipart("multipart/*").to("direct:validate-tenant").process(exchange -> {
            String fileName = System.currentTimeMillis() + "_" + exchange.getIn().getHeader("fileName", String.class);
            String requestId = exchange.getIn().getHeader("requestId", String.class);
            String purpose = exchange.getIn().getHeader("purpose", String.class);
            String batchId = UUID.randomUUID().toString();
            exchange.setProperty(BATCH_ID, batchId);
            exchange.setProperty(FILE_NAME, fileName);
            exchange.setProperty(REQUEST_ID, requestId);
            exchange.setProperty(PURPOSE, purpose);
        }).wireTap("direct:start-batch-process-csv").to("direct:pollingOutput");

        from("direct:validate-tenant")
                .id("direct:validate-tenant")
                .log("Validating tenant")
                .process(exchange -> {
                    String tenantName = exchange.getIn().getHeader(HEADER_PLATFORM_TENANT_ID, String.class);
                    // validation is disabled for now
                    /*if (tenantName == null || tenantName.isEmpty() || !tenants.contains(tenantName)) {
                        throw new Exception("Invalid tenant value.");
                    }*/
                    exchange.setProperty(TENANT_NAME, tenantName);
                })
                .setHeader("Content-Type", constant("application/json;charset=UTF-8"))
                .log("Completed route direct:validate-tenant");

        // this route is responsible for editing the incoming records based on configuration
        // this step is done to make sure the file format of CSV is not altered and only the data is updated based on
        // config
        from("direct:update-incoming-data").id("direct:update-incoming-data").log("direct:update-incoming-data")
                // [LOCAL_FILE_PATH] is already set in [direct:validateFileSyncResponse] route
                .setProperty(LOCAL_FILE_PATH, exchangeProperty(FILE_NAME)).to("direct:get-transaction-array")
                // make sure new data is set under the exchange variable [RESULT_TRANSACTION_LIST]
                .process(exchange -> {
                    String registeringInstituteId = exchange.getProperty(REGISTERING_INSTITUTE_ID, String.class);
                    String programId = exchange.getProperty(PROGRAM_ID, String.class);
                    logger.debug("Inst id: {}, prog id: {}", registeringInstituteId, programId);
                    if (!(StringUtils.hasText(registeringInstituteId) && StringUtils.hasText(programId))) {
                        // this will make sure the file is not updated since there is no update in data
                        logger.debug("InstitutionId or programId is null");

                        exchange.setProperty(IS_UPDATED, false);
                        return;
                    }
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    logger.debug("Size: {}", transactionList.size());
                    RegisteringInstitutionConfig registeringInstitutionConfig = budgetAccountConfig
                            .getByRegisteringInstituteId(registeringInstituteId);
                    if (registeringInstitutionConfig == null) {
                        logger.debug("Element in nested in config: {}", budgetAccountConfig.getRegisteringInstitutions().get(0).getPrograms().size());
                        logger.debug("Registering institute id is null");

                        exchange.setProperty(IS_UPDATED, false);
                        return;
                    }
                    Program program = registeringInstitutionConfig.getByProgramId(programId);
                    if (program == null) {
                        // this will make sure the file is not updated since there is no update in data
                        logger.debug("Program is null");
                        exchange.setProperty(IS_UPDATED, false);
                        return;
                    }
                    List<Transaction> resultTransactionList = new ArrayList<>();

                    transactionList.forEach(transaction -> {
                        transaction.setPayerIdentifierType(program.getIdentifierType());
                        transaction.setPayerIdentifier(program.getIdentifierValue());
                        resultTransactionList.add(transaction);
                        try {
                            logger.debug("Txn: {}", objectMapper.writeValueAsString(transaction));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    exchange.setProperty(RESULT_TRANSACTION_LIST, resultTransactionList);
                    exchange.setProperty(IS_UPDATED, true);
                    exchange.setProperty(PROGRAM_NAME, program.getName());
                    exchange.setProperty(PAYER_IDENTIFIER_TYPE, program.getIdentifierType());
                    exchange.setProperty(PAYER_IDENTIFIER_VALUE, program.getIdentifierValue());
                }).choice()
                // update only when previous(edit function) makes any changes to data
                .when(exchange -> exchange.getProperty(IS_UPDATED, Boolean.class))
                // warning: changing this flag can break things
                .setProperty(OVERRIDE_HEADER, constant(true)) // default header in CSV file will be used
                .to("direct:update-file-v2")
                .otherwise()
                .log(LoggingLevel.INFO, "No update");

        from("direct:start-batch-process-csv").id("direct:start-batch-process-csv").log("Starting route direct:start-batch-process-csv")
                .to("direct:update-incoming-data").process(exchange -> {
                    String fileName = exchange.getProperty(FILE_NAME, String.class);
                    String requestId = exchange.getProperty(REQUEST_ID, String.class);
                    String purpose = exchange.getProperty(PURPOSE, String.class);
                    String batchId = exchange.getProperty(BATCH_ID, String.class);
                    String note = null;

                    if (purpose == null || purpose.isEmpty()) {
                        purpose = "test payment";
                    }

                    logger.debug("\n\n Filename: {}", fileName);
                    logger.debug("\n\n BatchId: {} ", batchId);

                    File file = new File(fileName);
                    file.setWritable(true);
                    file.setReadable(true);
                    
                    logger.debug("File absolute path: {}", file.getAbsolutePath());

                    boolean verifyData = verifyData(file);
                    logger.debug("Data verification result {}", verifyData);
                    if (!verifyData) {
                        note = "Invalid data in file data processing stopped";
                    }

                    String nm = fileTransferService.uploadFile(file, bucketName);

                    logger.debug("File uploaded {}", nm);

                    // extracting and setting callback Url
                    String callbackUrl = exchange.getIn().getHeader("X-Callback-URL", String.class);
                    exchange.setProperty(CALLBACK_URL, callbackUrl);

                    List<Integer> phases = phaseUtils.getValues();
                    logger.debug(phases.toString());
                    Map<String, Object> variables = new HashMap<>();
                    variables.put(BATCH_ID, batchId);
                    variables.put(FILE_NAME, fileName);
                    variables.put(REQUEST_ID, requestId);
                    variables.put(PURPOSE, purpose);
                    variables.put(TENANT_ID, exchange.getProperty(TENANT_NAME));
                    variables.put(CALLBACK_URL, callbackUrl);
                    variables.put(PHASES, phases);
                    variables.put(PHASE_COUNT, phases.size());
                    variables.put(NOTE, note);
                    variables.put(CLIENT_CORRELATION_ID, exchange.getProperty(CLIENT_CORRELATION_ID));
                    variables.put(PROGRAM_NAME, exchange.getProperty(PROGRAM_NAME));
                    variables.put(PAYER_IDENTIFIER_TYPE, exchange.getProperty(PAYER_IDENTIFIER_TYPE));
                    variables.put(PAYER_IDENTIFIER_VALUE, exchange.getProperty(PAYER_IDENTIFIER_VALUE));
                    variables.put(REGISTERING_INSTITUTE_ID, exchange.getProperty(REGISTERING_INSTITUTE_ID));
                    variables.put(IS_FILE_VALID, true);
                    setConfigProperties(variables);

					logger.debug("Zeebe variables published: {}", variables);
                    log.debug("Variables published to zeebe: {}", variables);

                    JSONObject response = new JSONObject();

                    try {
                        String tenantSpecificWorkflowId = workflowId.replace("{dfspid}", exchange.getProperty(TENANT_NAME).toString());
                        String txnId = zeebeProcessStarter.startZeebeWorkflow(tenantSpecificWorkflowId, "", variables);
                        if (txnId == null || txnId.isEmpty()) {
                            logger.debug("error: Issue in starting the zeebe workflow, check the zeebe configuration");
                            response.put("errorCode", 500);
                            response.put("errorDescription", "Unable to start zeebe workflow");
                            response.put("developerMessage", "Issue in starting the zeebe workflow, check the zeebe configuration");
                        } else {
                            logger.debug("successful: zeebe workflow started {}",txnId);
                            response.put("batch_id", batchId);
                            response.put("request_id", requestId);
                            response.put("status", "queued");
                        }
                    } catch (Exception e) {
                        logger.debug("error: Issue in starting the zeebe workflow {}", e.getLocalizedMessage());
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
                    String clientCorrelationId = exchange.getIn().getHeader(HEADER_CLIENT_CORRELATION_ID, String.class);
                    String registeringInstitutionId = exchange.getIn().getHeader(HEADER_REGISTERING_INSTITUTE_ID, String.class);
                    String programId = exchange.getIn().getHeader(HEADER_PROGRAM_ID, String.class);
                    exchange.setProperty(FILE_NAME, filename);
                    exchange.setProperty(REQUEST_ID, requestId);
                    exchange.setProperty(PURPOSE, purpose);
                    exchange.setProperty(BATCH_REQUEST_TYPE, type);
                    exchange.setProperty(CLIENT_CORRELATION_ID, clientCorrelationId);
                    exchange.setProperty(REGISTERING_INSTITUTE_ID, registeringInstitutionId);
                    exchange.setProperty(PROGRAM_ID, programId);
                })
                .choice()
                .when(exchange -> exchange.getProperty(BATCH_REQUEST_TYPE, String.class).equalsIgnoreCase("raw"))
                .to("direct:start-batch-process-raw")
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

    private boolean verifyData(File file) throws IOException {
        InputStream ips = new FileInputStream(file);
        InputStreamReader ipsr = new InputStreamReader(ips);
        BufferedReader br = new BufferedReader(ipsr);
        String line;
        br.readLine();
        while ((line = br.readLine()) != null) {
            String[] row = line.split(",", -1);
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

    private boolean verifyCsv(File csvData) throws IOException {
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
        variables.put(BATCH_AGGREGATE_ENABLED, workerConfig.isBatchAggregateEnabled);
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
