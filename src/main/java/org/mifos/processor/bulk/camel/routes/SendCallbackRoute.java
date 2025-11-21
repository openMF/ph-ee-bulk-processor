package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.CALLBACK_RESPONSE_CODE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK_SUCCESS;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_RATE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_CODE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_DESCRIPTION;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MAX_CALLBACK_RETRY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PHASES;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.mifos.processor.bulk.schema.BatchCallbackDTO;
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
                .log("Sending callback for Batch Processing").setHeader(Exchange.HTTP_METHOD, constant("POST")).process(exchange -> {
                    String message = String.format("The Batch Aggregation API was complete with : %s",
                            exchange.getProperty(COMPLETION_RATE).toString());
                    callbackUrl = exchange.getProperty(CALLBACK, String.class);
                    logger.info("Callback URL: {}", callbackUrl);
                    logger.info("Callback Body: {}", message);
                    String batchId = exchange.getProperty(BATCH_ID, String.class);
                    String clientCorrelationId = exchange.getProperty(CLIENT_CORRELATION_ID, String.class);
                    BatchCallbackDTO batchCallbackDTO = new BatchCallbackDTO(clientCorrelationId, batchId, message);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonString = objectMapper.writeValueAsString(batchCallbackDTO);
                    exchange.getIn().setBody(jsonString);
                }).choice().when(exchangeProperty("X-CallbackURL").isNotNull()).setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .toD("${exchangeProperty.X-CallbackURL}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log(LoggingLevel.INFO, "Callback Response body: ${body}").endChoice().otherwise()
                .log("Unable to send callback: callback url is null").choice().when(header(Exchange.HTTP_RESPONSE_CODE).regex("^2\\d{2}$"))
                .when(exchangeProperty("X-CallbackURL").isNotNull()).log(LoggingLevel.INFO, "Callback sending was successful")
                .process(exchange -> {
                    List phases = exchange.getProperty(PHASES, List.class);
                    exchange.setProperty(CALLBACK_RESPONSE_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                    exchange.setProperty(CALLBACK_RETRY, 1);
                    exchange.setProperty(CALLBACK_SUCCESS, true);
                    eliminatePhases(exchange);
                }).otherwise().log(LoggingLevel.ERROR, "Callback request was unsuccessful").process(exchange -> {
                    int retry = exchange.getProperty(CALLBACK_RETRY, Integer.class);
                    int maxRetry = exchange.getProperty(MAX_CALLBACK_RETRY, Integer.class);
                    if (retry >= maxRetry) {
                        List<Integer> phases = exchange.getProperty(PHASES, List.class);
                        logger.info("Retry Exhausted, setting Callback as Failed");
                        eliminatePhases(exchange);
                        exchange.setProperty(CALLBACK_RESPONSE_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                        exchange.setProperty(CALLBACK_SUCCESS, false);
                        exchange.setProperty(ERROR_DESCRIPTION, exchange.getIn().getBody(String.class));
                        exchange.setProperty(ERROR_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                    } else {
                        retry++;
                        logger.info("Retry Left {}, Setting Callback as Failed and Retrying...", (maxRetry - retry));
                        exchange.setProperty(CALLBACK_RETRY, retry);

                    }
                    exchange.setProperty(CALLBACK_RESPONSE_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                    exchange.setProperty(CALLBACK_SUCCESS, false);
                    exchange.setProperty(ERROR_DESCRIPTION, exchange.getIn().getBody(String.class));
                    exchange.setProperty(ERROR_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));

                });
    }

    public void eliminatePhases(Exchange exchange) {
        List<Integer> phases = exchange.getProperty(PHASES, List.class);
        int completionRate = exchange.getProperty(COMPLETION_RATE, Integer.class);

        phases.removeIf(phase -> phase <= completionRate);

        exchange.setProperty(PHASES, phases);
    }

}
