package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.camel.config.CamelProperties.BATCH_STATUS_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_RATE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_CODE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_DESCRIPTION;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_CALLBACK_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_STATUS_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASES;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASE_COUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.stereotype.Component;

@Component
public class AggregateWorker extends BaseWorker {

    @Override
    public void setup() {
        // newWorker(Worker.BATCH_STATUS, (client, job) -> {
        newWorker(Worker.BATCH_AGGREGATE, (client, job) -> {
            logger.info("Started batchAggregateWorker");
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();

            int retry = (int) variables.getOrDefault(RETRY, 0);
            int successRate = (int) variables.getOrDefault(COMPLETION_RATE, 0);

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(CLIENT_CORRELATION_ID, variables.get(CLIENT_CORRELATION_ID));
            exchange.setProperty(BATCH_ID, variables.get(BATCH_ID));
            exchange.setProperty(TENANT_ID, variables.get(TENANT_ID));
            exchange.setProperty(MAX_CALLBACK_RETRY, variables.get(MAX_CALLBACK_RETRY));
            exchange.setProperty(CALLBACK_RETRY, variables.getOrDefault(CALLBACK_RETRY, 0));
            exchange.setProperty(CALLBACK, variables.get(CALLBACK));
            // exchange.setProperty(COMPLETION_RATE, variables.get(COMPLETION_RATE));
            exchange.setProperty(PHASES, variables.get(PHASES));
            exchange.setProperty(PHASE_COUNT, variables.get(PHASE_COUNT));
            sendToCamelRoute(RouteId.BATCH_AGGREGATE, exchange);

            Boolean batchStatusFailed = exchange.getProperty(BATCH_STATUS_FAILED, Boolean.class);
            // if (batchStatusFailed == null || !batchStatusFailed) {
            if (exchange.getException() != null && exchange.getException().getMessage() != null
                    && exchange.getException().getMessage().contains("404")) {
                logger.error("An error occurred, retrying");
                successRate = 0;
                variables.put(ERROR_CODE, exchange.getProperty(ERROR_CODE));
                variables.put(ERROR_DESCRIPTION, exchange.getProperty(ERROR_DESCRIPTION));
                logger.info("Retry: {} , Error cause: {}, message: {}",retry, exchange.getException().getCause(), exchange.getException().getMessage());
            } else {
                logger.info("BATCH SUCCESS  retry: {} , and maxRetry: {}",retry,variables.get(MAX_STATUS_RETRY));
                successRate = exchange.getProperty(COMPLETION_RATE, Long.class).intValue();
            }
            // } else {

            // }

            variables.put(COMPLETION_RATE, successRate);
            variables.put(RETRY, ++retry);

            logger.info("Retry: {} and Success Rate: {}", retry, successRate);
            client.newCompleteCommand(job.getKey()).variables(variables).send();
            logger.info("Completed batchAggregateWorker");
        });
    }

}
