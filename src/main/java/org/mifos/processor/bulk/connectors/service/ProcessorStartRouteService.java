package org.mifos.processor.bulk.connectors.service;

import static org.mifos.processor.bulk.camel.config.CamelProperties.BATCH_REQUEST_TYPE;
import static org.mifos.processor.bulk.camel.config.CamelProperties.CONTENT_TYPE;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PLATFORM_TENANT_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_PROGRAM_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PROGRAM_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TENANT_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.json.JSONObject;
import org.mifos.processor.bulk.camel.routes.ProcessorStartRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProcessorStartRouteService {

    @Autowired
    ProcessorStartRoute processorStartRoute;
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
}
