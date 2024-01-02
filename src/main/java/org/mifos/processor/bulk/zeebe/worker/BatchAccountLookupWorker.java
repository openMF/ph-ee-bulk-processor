package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.camel.config.CamelProperties.CACHED_TRANSACTION_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_LOOKUP_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;
import static org.mifos.processor.bulk.zeebe.worker.Worker.BATCH_ACCOUNT_LOOKUP;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    @Value("${identity_account_mapper.hostname}")
    private String identityMapperURL;
    @Value("${bulk-processor.hostname}")
    private String bulkURL;
    @Value("${identity_account_mapper.batch_account_lookup_callback}")
    private String batchAccountLookupCallback;

    @Override
    public void setup() {

        newWorker(BATCH_ACCOUNT_LOOKUP, ((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();
            Exchange exchange = new DefaultExchange(camelContext);
            String filename = (String) variables.get(FILE_NAME);
            String registeringInstituteId = variables.get(REGISTERING_INSTITUTE_ID).toString();
            logger.info("registeringInstituteId in worker {}", registeringInstituteId);
            variables.put(CACHED_TRANSACTION_ID, job.getKey());
            exchange.setProperty(HEADER_REGISTERING_INSTITUTE_ID, registeringInstituteId);
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
