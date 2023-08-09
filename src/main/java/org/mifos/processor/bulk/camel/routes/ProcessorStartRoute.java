package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.BATCH_REQUEST_TYPE;
import static org.mifos.processor.bulk.camel.config.CamelProperties.IS_UPDATED;
import static org.mifos.processor.bulk.camel.config.CamelProperties.OVERRIDE_HEADER;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PROGRAM_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.RESULT_TRANSACTION_LIST;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TENANT_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.APPROVAL_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BULK_NOTIF_FAILURE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BULK_NOTIF_SUCCESS;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_URL;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_THRESHOLD;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_THRESHOLD_CHECK_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FORMATTING_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_CALLBACK_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_STATUS_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MERGE_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ORDERING_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_LOOKUP_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYER_IDENTIFIER_TYPE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYER_IDENTIFIER_VALUE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASES;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASE_COUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PROGRAM_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.SPLITTING_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.THRESHOLD_DELAY;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
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
                .unmarshal().mimeMultipart("multipart/*").to("direct:validate-tenant").process(exchange -> {
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

        from("direct:post-bulk-transfer").unmarshal().mimeMultipart("multipart/*").to("direct:validate-tenant").process(exchange -> {
            String fileName = System.currentTimeMillis() + "_" + exchange.getIn().getHeader("fileName", String.class);
            String requestId = exchange.getIn().getHeader("requestId", String.class);
            String purpose = exchange.getIn().getHeader("purpose", String.class);

            exchange.setProperty(FILE_NAME, fileName);
            exchange.setProperty(REQUEST_ID, requestId);
            exchange.setProperty(PURPOSE, purpose);
        }).to("direct:start-batch-process-csv");

        from("direct:validate-tenant").id("direct:validate-tenant").log("Validating tenant").process(exchange -> {
            String tenantName = exchange.getIn().getHeader("Platform-TenantId", String.class);
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
                .to("direct:get-transaction-array")
                // make sure new data is set under the exchange variable [RESULT_TRANSACTION_LIST]
                .process(exchange -> {
                    String registeringInstituteId = exchange.getProperty(REGISTERING_INSTITUTE_ID, String.class);
                    String programId = exchange.getProperty(PROGRAM_ID, String.class);
                    logger.debug("Inst id: {}, prog id: {}", registeringInstituteId, programId);
                    if (!(StringUtils.hasText(registeringInstituteId) && StringUtils.hasText(programId))) {
                        // this will make sure the file is not updated since there is no update in data
                        logger.debug("Reh or pro is null");
                        exchange.setProperty(IS_UPDATED, false);
                        return;
                    }
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    logger.debug("Size: {}", transactionList.size());
                    RegisteringInstitutionConfig registeringInstitutionConfig = budgetAccountConfig
                            .getByRegisteringInstituteId(registeringInstituteId);
                    if (registeringInstitutionConfig == null) {
                        logger.debug("Element in nested in config: {}",
                                budgetAccountConfig.getRegisteringInstitutions().get(0).getPrograms().size());
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
                .to("direct:update-file-v2").otherwise().log(LoggingLevel.DEBUG, "No update");

        from("direct:start-batch-process-csv").id("direct:start-batch-process-csv").log("Starting route direct:start-batch-process-csv")
                .to("direct:update-incoming-data").process(exchange -> {
                    String fileName = exchange.getProperty(FILE_NAME, String.class);
                    String requestId = exchange.getProperty(REQUEST_ID, String.class);
                    String purpose = exchange.getProperty(PURPOSE, String.class);
                    String batchId = UUID.randomUUID().toString();

                    if (purpose == null || purpose.isEmpty()) {
                        purpose = "test payment";
                    }

                    logger.debug("\n\n Filename: {}", fileName);
                    logger.debug("\n\n BatchId: {} ", batchId);

                    File file = new File(fileName);
                    file.setWritable(true);
                    file.setReadable(true);

                    String csvData = exchange.getIn().getBody(String.class);
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(csvData);
                    fileWriter.close();

                    logger.info("" + file.length());
                    logger.info(file.getAbsolutePath());

                    String nm = fileTransferService.uploadFile(file, bucketName);

                    logger.debug("File uploaded {}", nm);

                    // extracting and setting callback Url
                    String callbackUrl = exchange.getIn().getHeader("X-Callback-URL", String.class);
                    exchange.setProperty(CALLBACK_URL, callbackUrl);

                    List<Integer> phases = phaseUtils.getValues();
                    logger.info(phases.toString());
                    Map<String, Object> variables = new HashMap<>();
                    variables.put(BATCH_ID, batchId);
                    variables.put(FILE_NAME, fileName);
                    variables.put(REQUEST_ID, requestId);
                    variables.put(PURPOSE, purpose);
                    variables.put(TENANT_ID, exchange.getProperty(TENANT_NAME));
                    variables.put(CALLBACK_URL, callbackUrl);
                    variables.put(PHASES, phases);
                    variables.put(PHASE_COUNT, phases.size());
                    variables.put(PROGRAM_NAME, exchange.getProperty(PROGRAM_NAME));
                    variables.put(PAYER_IDENTIFIER_TYPE, exchange.getProperty(PAYER_IDENTIFIER_TYPE));
                    variables.put(PAYER_IDENTIFIER_VALUE, exchange.getProperty(PAYER_IDENTIFIER_VALUE));
                    variables.put(REGISTERING_INSTITUTE_ID, exchange.getProperty(REGISTERING_INSTITUTE_ID));
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
