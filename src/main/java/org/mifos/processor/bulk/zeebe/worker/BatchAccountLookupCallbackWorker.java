package org.mifos.processor.bulk.zeebe.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;
import static org.mifos.processor.bulk.zeebe.worker.Worker.BATCH_ACCOUNT_LOOKUP_CALLBACK;

@Component
public class BatchAccountLookupCallbackWorker extends BaseWorker{
    @Autowired
    private ZeebeClient zeebeClient;
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Override
    public void setup() {
        logger.info("## generating " + BATCH_ACCOUNT_LOOKUP_CALLBACK + "zeebe worker");
        newWorker(BATCH_ACCOUNT_LOOKUP_CALLBACK, ((client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();
            Exchange exchange = new DefaultExchange(camelContext);
            String filename = (String) variables.get(FILE_NAME);
            String batchAccountLookupCallback = (String) variables.get("batchAccountLookupCallback");
            variables.put(PARTY_LOOKUP_FAILED, false);
            exchange.setProperty(SERVER_FILE_NAME, filename);
            exchange.setProperty("batchAccountLookupCallback", batchAccountLookupCallback);
            exchange.setProperty("workflowInstanceKey",job.getProcessInstanceKey());
            sendToCamelRoute(RouteId.ACCOUNT_LOOKUP_CALLBACK, exchange);
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        }));
    }
}
