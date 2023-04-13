package org.mifos.processor.bulk.zeebe.worker;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class OrderingWorker extends BaseWorker {

    @Override
    public void setup() {

        /**
         * This worker is responsible for ordering the data set based on field configuration. Performs below tasks.
         * 1. Downloads the file from cloud.
         * 2. Parse the data into POJO.
         * 3. Re-order the data based on field configured in application.yaml
         * 4. Uploads the updated file in cloud
         */
        newWorker(Worker.ORDERING, (client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();
            if (workerConfig.isOrderingWorkerEnabled) {
                variables.put(ORDERING_FAILED, false);
            }

            String filename = (String) variables.get(FILE_NAME);
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(SERVER_FILE_NAME, filename);

            try {
                sendToCamelRoute(RouteId.ORDERING, exchange);
                assert !exchange.getProperty(ORDERING_FAILED, Boolean.class);
            } catch (Exception e) {
                variables.put(ORDERING_FAILED, true);
            }

            variables.put(ORDERING_FAILED, false);
            variables.put(ORDERED_BY, exchange.getProperty(ORDERED_BY));

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
