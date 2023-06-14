package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FORMATTING_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FORMATTING_STANDARD;

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.stereotype.Component;

@Component
public class FormattingWorker extends BaseWorker {

    @Override
    public void setup() {

        /**
         * Starts the new worker for formatting of the data. Performs below tasks 1. Downloads the file from cloud. 2.
         * Parse the data into POJO. 3. Format the data based on field configured in application.yaml 4. Uploads the
         * updated file in cloud
         */
        newWorker(Worker.FORMATTING, (client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();
            if (workerConfig.isFormattingWorkerEnabled) {
                variables.put(FORMATTING_FAILED, false);
            }

            String filename = (String) variables.get(FILE_NAME);
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(SERVER_FILE_NAME, filename);

            try {
                sendToCamelRoute(RouteId.FORMATTING, exchange);
                assert !exchange.getProperty(FORMATTING_FAILED, Boolean.class);
            } catch (Exception e) {
                variables.put(FORMATTING_FAILED, true);
            }

            variables.put(FORMATTING_FAILED, false);
            variables.put(FORMATTING_STANDARD, exchange.getProperty(FORMATTING_STANDARD));

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
