package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.DE_DUPLICATION_ENABLE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.DE_DUPLICATION_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.DUPLICATE_TRANSACTION_COUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.DUPLICATE_TRANSACTION_FILE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.stereotype.Component;

@Component
public class DeDuplicationWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.DE_DEPLICATION, (client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            logger.info("Started {} worker", Worker.DE_DEPLICATION.getValue());
            Map<String, Object> variables = job.getVariablesAsMap();

            if ((Boolean) variables.get(DE_DUPLICATION_ENABLE)) {
                variables.put(DE_DUPLICATION_FAILED, false);
            }

            Exchange exchange = new DefaultExchange(camelContext);

            String filename = (String) variables.get(FILE_NAME);

            logger.info("Filename in worker before duplication is: {}", filename);

            exchange.setProperty(SERVER_FILE_NAME, filename);

            sendToCamelRoute(RouteId.DE_DUPLICATION, exchange);

            boolean deDuplicationFailed = (Boolean) exchange.getProperty(DE_DUPLICATION_FAILED);
            int duplicateTransactionCount = exchange.getProperty(DUPLICATE_TRANSACTION_COUNT, Integer.class);
            if (duplicateTransactionCount > 0) {
                // if duplicate txn exist
                variables.put(DUPLICATE_TRANSACTION_FILE, exchange.getProperty(DUPLICATE_TRANSACTION_FILE, String.class));
            }
            variables.put(DE_DUPLICATION_FAILED, deDuplicationFailed);
            variables.put(DUPLICATE_TRANSACTION_COUNT, duplicateTransactionCount);

            logger.debug("Zeebe variables in dedup: {}", variables);
            client.newCompleteCommand(job.getKey()).variables(variables).send();
            logger.info("Completed {} worker", Worker.DE_DEPLICATION);
        });

    }
}
