package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.CALLBACK_RESPONSE_CODE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_SUCCESS;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_URL;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_RATE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_CODE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_DESCRIPTION;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_CALLBACK_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASES;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASE_COUNT;

import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SendCallbackRoute extends BaseRouteBuilder {

    @Value("${callback.url}")
    private String callbackUrl;

    @Override
    public void configure() throws Exception {

        from("rest:get:test/send/callback").to(RouteId.SEND_CALLBACK.getValue());

        /**
         * Base route for kicking off callback. Performs below tasks. Sends Callback to the set url Checks of response
         * code is anything not 2xx then retries
         */

        from(RouteId.SEND_CALLBACK.getValue()).id(RouteId.SEND_CALLBACK.getValue()).log("Starting route " + RouteId.SEND_CALLBACK.name())
                .log("Sending callback for Batch Processing").setBody(exchange -> {
                    String body = String.format("The Batch Aggregation API was complete with : %s",
                            exchange.getProperty(COMPLETION_RATE).toString());
                    callbackUrl = exchange.getProperty(CALLBACK_URL).toString();
                    return body;
                }).toD("${header.callbackUrl}?bridgeEndpoint=true&throwExceptionOnFailure=false").choice()
                .when(header("CamelHttpResponseCode").startsWith("2")).log(LoggingLevel.INFO, "Callback sending was successful")
                .process(exchange -> {
                    List<Integer> phases = (List<Integer>) exchange.getProperty(PHASES);

                    exchange.setProperty(CALLBACK_RESPONSE_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                    exchange.setProperty(CALLBACK_RETRY, 1);
                    exchange.setProperty(CALLBACK_SUCCESS, true);
                    eliminatePhases(exchange, phases, (Integer) exchange.getProperty(PHASE_COUNT),
                            (Integer) exchange.getProperty(COMPLETION_RATE));

                }).otherwise().log(LoggingLevel.ERROR, "Callback request was unsuccessful").process(exchange -> {
                    if (exchange.getProperty(CALLBACK_RETRY).equals(exchange.getProperty(MAX_CALLBACK_RETRY))) {
                        List<Integer> phases = (List<Integer>) exchange.getProperty(PHASES);
                        logger.info("Retry Exhausted, setting Callback as Failed");
                        eliminatePhases(exchange, phases, (Integer) exchange.getProperty(PHASE_COUNT),
                                (Integer) exchange.getProperty(COMPLETION_RATE));
                        exchange.setProperty(CALLBACK_RESPONSE_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                    } else {
                        int retry = (int) exchange.getProperty(CALLBACK_RETRY);
                        retry++;
                        logger.info("Retry Left {}, Setting Callback as Failed and Retrying...",
                                ((int) exchange.getProperty(MAX_CALLBACK_RETRY) - retry));
                        exchange.setProperty(CALLBACK_RETRY, retry);
                        exchange.setProperty(CALLBACK_RESPONSE_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));

                    }
                    exchange.setProperty(CALLBACK_SUCCESS, false);
                    exchange.setProperty(ERROR_DESCRIPTION, exchange.getIn().getBody(String.class));
                    exchange.setProperty(ERROR_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                });

        // for temporary callback simulation
        from("rest:post:/simulate").log("Reached Simulation");
    }

    public void eliminatePhases(Exchange exchange, List<Integer> phases, int phaseCount, int completionRate) {
        int i = 0;
        while (phases.size() > 0 && phases.size() > i) {
            if (phases.get(i) <= completionRate) phases.remove(i);
            i++;
        }
        exchange.setProperty(PHASES, phases);
        exchange.setProperty(PHASE_COUNT, phaseCount);
    }

}
