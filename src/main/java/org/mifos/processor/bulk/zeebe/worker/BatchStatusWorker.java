package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.camel.config.CamelProperties.BATCH_STATUS_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_RATE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_CODE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_DESCRIPTION;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.stereotype.Component;

@Component
public class BatchStatusWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.BATCH_STATUS, (client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();

            int retry = (int) variables.getOrDefault(RETRY, 0);
            int successRate = (int) variables.getOrDefault(COMPLETION_RATE, 0);

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(BATCH_ID, variables.get(BATCH_ID));
            exchange.setProperty(TENANT_ID, variables.get(TENANT_ID));

            sendToCamelRoute(RouteId.BATCH_STATUS, exchange);

            Boolean batchStatusFailed = exchange.getProperty(BATCH_STATUS_FAILED, Boolean.class);
            if (batchStatusFailed == null || !batchStatusFailed) {
                successRate = exchange.getProperty(COMPLETION_RATE, Integer.class);
            } else {
                variables.put(ERROR_CODE, exchange.getProperty(ERROR_CODE));
                variables.put(ERROR_DESCRIPTION, exchange.getProperty(ERROR_DESCRIPTION));
                logger.info("Error: {}, {}", variables.get(ERROR_CODE), variables.get(ERROR_DESCRIPTION));
            }

            variables.put(COMPLETION_RATE, successRate);
            variables.put(RETRY, ++retry);

            logger.info("Retry: {} and Success Rate: {}", retry, successRate);
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
