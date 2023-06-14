package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.camel.config.CamelProperties.CALLBACK_RESPONSE_CODE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_SUCCESS;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_URL;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_RATE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_CODE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_DESCRIPTION;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_CALLBACK_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_STATUS_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASES;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASE_COUNT;

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.stereotype.Component;

@Component
public class SendCallbackWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.SEND_CALLBACK, (client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();

            int retry = variables.getOrDefault(CALLBACK_RETRY, 0).equals(variables.get(MAX_STATUS_RETRY)) ? 0
                    : (int) variables.getOrDefault(CALLBACK_RETRY, 0);
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(MAX_CALLBACK_RETRY, variables.get(MAX_CALLBACK_RETRY));
            exchange.setProperty(CALLBACK_RETRY, retry);
            exchange.setProperty(CALLBACK_URL, variables.get(CALLBACK_URL));
            exchange.setProperty(COMPLETION_RATE, variables.get(COMPLETION_RATE));
            exchange.setProperty(PHASES, variables.get(PHASES));
            exchange.setProperty(PHASE_COUNT, variables.get(PHASE_COUNT));
            sendToCamelRoute(RouteId.SEND_CALLBACK, exchange);

            Boolean callbackSuccess = exchange.getProperty(CALLBACK_SUCCESS, Boolean.class);
            if (callbackSuccess == null || !callbackSuccess) {
                variables.put(ERROR_CODE, exchange.getProperty(ERROR_CODE));
                variables.put(ERROR_DESCRIPTION, exchange.getProperty(ERROR_DESCRIPTION));
                logger.info("Error: {}, {}", variables.get(ERROR_CODE), variables.get(ERROR_DESCRIPTION));
            } else {
                variables.put(CALLBACK_SUCCESS, true);
            }

            variables.put(CALLBACK_RETRY, exchange.getProperty(CALLBACK_RETRY));
            variables.put(CALLBACK_RESPONSE_CODE, exchange.getProperty(CALLBACK_RESPONSE_CODE));
            variables.put(PHASE_COUNT, exchange.getProperty(PHASE_COUNT));
            variables.put(PHASES, exchange.getProperty(PHASES));

            logger.debug("Retry: {} and Response Code {}", exchange.getProperty(CALLBACK_RETRY),
                    exchange.getProperty(CALLBACK_RESPONSE_CODE));
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
