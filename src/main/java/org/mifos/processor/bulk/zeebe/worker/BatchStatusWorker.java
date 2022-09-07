package org.mifos.processor.bulk.zeebe.worker;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.stereotype.Component;
import java.util.Map;
import static org.mifos.processor.bulk.camel.config.CamelProperties.BATCH_STATUS_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class BatchStatusWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.BATCH_STATUS, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();

            int retry = (int) variables.getOrDefault(RETRY, 0);
            int successRate = (int) variables.getOrDefault(SUCCESS_RATE, 0);

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(BATCH_ID, variables.get(BATCH_ID));
            exchange.setProperty(TENANT_ID, variables.get(TENANT_ID));

            sendToCamelRoute(RouteId.BATCH_STATUS, exchange);

            Boolean batchStatusFailed = exchange.getProperty(BATCH_STATUS_FAILED, Boolean.class);
            if (batchStatusFailed == null || !batchStatusFailed) {
                successRate = exchange.getProperty(SUCCESS_RATE, Long.class).intValue();
            } else {
                variables.put(ERROR_CODE, exchange.getProperty(ERROR_CODE));
                variables.put(ERROR_DESCRIPTION, exchange.getProperty(ERROR_DESCRIPTION));
                logger.info("Error: {}, {}", variables.get(ERROR_CODE), variables.get(ERROR_DESCRIPTION));
            }

            variables.put(SUCCESS_RATE, successRate);
            variables.put(RETRY, ++retry);

            logger.info("Retry: {} and Success Rate: {}", retry, successRate);
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
