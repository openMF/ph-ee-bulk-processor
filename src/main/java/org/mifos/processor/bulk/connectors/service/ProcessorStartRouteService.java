package org.mifos.processor.bulk.connectors.service;

import static org.mifos.processor.bulk.camel.config.CamelProperties.BATCH_REQUEST_TYPE;
import static org.mifos.processor.bulk.camel.config.CamelProperties.CONTENT_TYPE;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PLATFORM_TENANT_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PROGRAM_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.IS_UPDATED;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PROGRAM_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.RESULT_TRANSACTION_LIST;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TENANT_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_URL;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.IS_FILE_VALID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.NOTE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYEE_DFSP_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYER_IDENTIFIER_TYPE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYER_IDENTIFIER_VALUE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASES;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASE_COUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PROGRAM_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.api.command.ClientStatusException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.json.JSONObject;
import org.mifos.processor.bulk.camel.routes.ProcessorStartRoute;
import org.mifos.processor.bulk.config.BudgetAccountConfig;
import org.mifos.processor.bulk.config.Program;
import org.mifos.processor.bulk.config.RegisteringInstitutionConfig;
import org.mifos.processor.bulk.file.FileTransferService;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.utility.PhaseUtils;
import org.mifos.processor.bulk.zeebe.ZeebeProcessStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProcessorStartRouteService {

    @Autowired
    ProcessorStartRoute processorStartRoute;
    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;
    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;
    @Autowired
    PhaseUtils phaseUtils;
    @Autowired
    public ObjectMapper objectMapper;
    @Autowired
    BudgetAccountConfig budgetAccountConfig;
    @Value("${application.bucket-name}")
    private String bucketName;
    @Value("${csv.size}")
    private int csvSize;
    @Value("${pollingApi.path}")
    private String pollApiPath;
    @Value("${pollingApi.timer}")
    private String pollApiTimer;

    public Logger logger = LoggerFactory.getLogger(this.getClass());

    public void validateFileSyncResponse(Exchange exchange) throws IOException {
        String fileName = exchange.getIn().getHeader(FILE_NAME, String.class);
        File file = new File(fileName);

        // check the file structure
        int fileSize = (int) file.length();
        if (fileSize > csvSize) {
            processorStartRoute.setErrorResponse(exchange, 400, "File too big",
                    "The file uploaded is too big. " + "Please upload a file and try again.");
        } else if (!processorStartRoute.verifyCsv(file)) {
            processorStartRoute.setErrorResponse(exchange, 400, "Invalid file structure",
                    "The file uploaded contains wrong structure." + " Please upload correct file columns and try again.");
        } else {
            logger.debug("Filename: {}", fileName);
            processorStartRoute.setResponse(exchange, 200);
        }
    }

    public void validateTenant(Exchange exchange) {
        String tenantName = exchange.getIn().getHeader(HEADER_PLATFORM_TENANT_ID, String.class);
        // validation is disabled for now
        /*
         * if (tenantName == null || tenantName.isEmpty() || !tenants.contains(tenantName)) { throw new
         * Exception("Invalid tenant value."); }
         */
        exchange.setProperty(TENANT_NAME, tenantName);
        exchange.getIn().setHeader(CONTENT_TYPE, "application/json;charset=UTF-8");
    }

    public void pollingOutput(Exchange exchange) {
        JSONObject json = new JSONObject();
        String pollingPath = String.format("%s%s", pollApiPath, exchange.getProperty(BATCH_ID));
        json.put("PollingPath", pollingPath);
        json.put("SuggestedCallbackSeconds", pollApiTimer);
        exchange.getIn().setBody(json.toString());
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 202);
    }

    public void executeBatch(Exchange exchange) {
        String filename = exchange.getIn().getHeader("filename", String.class);
        String requestId = exchange.getIn().getHeader("X-CorrelationID", String.class);
        String purpose = exchange.getIn().getHeader("Purpose", String.class);
        String type = exchange.getIn().getHeader("Type", String.class);
        String clientCorrelationId = exchange.getIn().getHeader(HEADER_CLIENT_CORRELATION_ID, String.class);
        String registeringInstitutionId = exchange.getIn().getHeader(HEADER_REGISTERING_INSTITUTE_ID, String.class);
        logger.info("registeringInstitutionId {}", registeringInstitutionId);
        String programId = exchange.getIn().getHeader(HEADER_PROGRAM_ID, String.class);
        String callbackUrl = exchange.getIn().getHeader("X-CallbackURL", String.class);
        exchange.setProperty(FILE_NAME, filename);
        exchange.setProperty(REQUEST_ID, requestId);
        exchange.setProperty(PURPOSE, purpose);
        exchange.setProperty(BATCH_REQUEST_TYPE, type);
        exchange.setProperty(CLIENT_CORRELATION_ID, clientCorrelationId);
        exchange.setProperty(REGISTERING_INSTITUTE_ID, registeringInstitutionId);
        exchange.setProperty(PROGRAM_ID, programId);
        exchange.setProperty(CALLBACK, callbackUrl);
    }

    public void startBatchProcessRaw(Exchange exchange) {
        JSONObject response = new JSONObject();
        response.put("batch_id", UUID.randomUUID().toString());
        response.put("request_id", UUID.randomUUID().toString());
        response.put("status", "queued");
        exchange.getIn().setBody(response.toString());
    }

    public void updateIncomingData(Exchange exchange) {
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
        RegisteringInstitutionConfig registeringInstitutionConfig = budgetAccountConfig.getByRegisteringInstituteId(registeringInstituteId);
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
    }

    public void startBatchProcessCsv(Exchange exchange) throws IOException {
        String fileName = exchange.getProperty(FILE_NAME, String.class);
        String requestId = exchange.getProperty(REQUEST_ID, String.class);
        String purpose = exchange.getProperty(PURPOSE, String.class);
        String batchId = exchange.getProperty(BATCH_ID, String.class);
        String callbackUrl = exchange.getProperty(CALLBACK, String.class);
        String payeeDfspId = exchange.getProperty(PAYEE_DFSP_ID, String.class);
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

        boolean verifyData = processorStartRoute.verifyData(file);
        logger.debug("Data verification result {}", verifyData);
        if (!verifyData) {
            note = "Invalid data in file data processing stopped";
        }

        String nm = fileTransferService.uploadFile(file, bucketName);

        logger.debug("File uploaded {}", nm);

        // extracting and setting callback Url
        exchange.setProperty(CALLBACK_URL, callbackUrl);

        List<Integer> phases = phaseUtils.getValues();
        logger.debug(phases.toString());
        Map<String, Object> variables = new HashMap<>();
        variables.put(BATCH_ID, batchId);
        variables.put(FILE_NAME, fileName);
        variables.put(REQUEST_ID, requestId);
        variables.put(PURPOSE, purpose);
        variables.put(TENANT_ID, exchange.getProperty(TENANT_NAME));
        variables.put(CALLBACK, callbackUrl);
        variables.put(PHASES, phases);
        variables.put(PHASE_COUNT, phases.size());
        variables.put(NOTE, note);
        variables.put(CLIENT_CORRELATION_ID, exchange.getProperty(CLIENT_CORRELATION_ID));
        variables.put(PROGRAM_NAME, exchange.getProperty(PROGRAM_NAME));
        variables.put(PAYER_IDENTIFIER_TYPE, exchange.getProperty(PAYER_IDENTIFIER_TYPE));
        variables.put(PAYER_IDENTIFIER_VALUE, exchange.getProperty(PAYER_IDENTIFIER_VALUE));
        variables.put(REGISTERING_INSTITUTE_ID, exchange.getProperty(REGISTERING_INSTITUTE_ID));
        variables.put(IS_FILE_VALID, true);
        variables.put(PAYEE_DFSP_ID, payeeDfspId);
        processorStartRoute.setConfigProperties(variables);

        logger.debug("Zeebe variables published: {}", variables);
        logger.debug("Variables published to zeebe: {}", variables);

        JSONObject response = new JSONObject();
        String bpmn = processorStartRoute.getWorkflowForTenant(exchange.getProperty(TENANT_NAME).toString(), "batch-transactions");

        try {
            String tenantSpecificWorkflowId = bpmn.replace("{dfspid}", exchange.getProperty(TENANT_NAME).toString());
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
        } catch (ClientStatusException c) {
            logger.error("Got ClientStatusException : {}", c.getMessage());
            throw c;
        } catch (Exception e) {
            response.put("errorCode", 500);
            response.put("errorDescription", "Unable to start zeebe workflow");
            response.put("developerMessage", e.getLocalizedMessage());
        }

        exchange.getIn().setBody(response.toString());
    }
}
