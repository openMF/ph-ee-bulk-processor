package org.mifos.processor.bulk.zeebe.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;
import static org.mifos.processor.bulk.zeebe.worker.Worker.BATCH_ACCOUNT_LOOKUP;

@Component
public class BatchAccountLookupWorker extends BaseWorker {
    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    private ProducerTemplate producerTemplate;
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${identity-account-mapper.hostname}")
    private String identityMapperURL;
    @Value("${bulk-processor.hostname}")
    private String bulkURL;
    @Value("${identity_account_mapper.endpoints.batch_account_lookup_callback}")
    private String batchAccountLookupCallback;

    @Override
    public void setup() {

        newWorker(BATCH_ACCOUNT_LOOKUP, ((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();
            Exchange exchange = new DefaultExchange(camelContext);
            String filename = (String) variables.get(FILE_NAME);
            variables.put(CACHED_TRANSACTION_ID, job.getKey());
            exchange.setProperty(HEADER_REGISTERING_INSTITUTE_ID, variables.get("registeringInstituteId").toString());
            exchange.setProperty(SERVER_FILE_NAME, filename);
            exchange.setProperty(REQUEST_ID, job.getKey());
            exchange.setProperty(CALLBACK, identityMapperURL + batchAccountLookupCallback);

            try {
                sendToCamelRoute(RouteId.ACCOUNT_LOOKUP, exchange);
            } catch (Exception e) {
                variables.put(PARTY_LOOKUP_FAILED, true);
            }
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        }));
    }
}
