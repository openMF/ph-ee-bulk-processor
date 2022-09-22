package org.mifos.processor.bulk.camel.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.spi.AsEndpointUri;
import org.mifos.processor.bulk.schema.BatchDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class SendCallbackRoute extends BaseRouteBuilder {


    @Value("${callback.url}")
    private String callbackUrl;

    @Override
    public void configure() throws Exception {

        from("rest:get:test/send/callback")
                .to(RouteId.SEND_CALLBACK.getValue());

        /**
         * Base route for kicking off callback. Performs below tasks.
         * Sends Callback to the set url
         * Checks of response code is anything not 2xx then retries
         */

        from(RouteId.SEND_CALLBACK.getValue())
                .id(RouteId.SEND_CALLBACK.getValue())
                .log("Starting route " + RouteId.SEND_CALLBACK.name())
                .log("Sending callback for Batch Processing")
                .setBody(exchange -> {
                    String body = "Batch Aggregation API was complete with %: " +
                            exchange.getProperty(SUCCESS_RATE);
                    callbackUrl = exchange.getProperty(CALLBACK_URL).toString();
                    return body;
                })
                .toD("${header.callbackUrl}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .choice()
                .when(header("CamelHttpResponseCode").startsWith("2"))
                .log(LoggingLevel.INFO, "Callback sending was successful")
                .process(exchange -> {
                    exchange.setProperty(CALLBACK_RESPONSE_CODE, exchange.getIn()
                            .getHeader(Exchange.HTTP_RESPONSE_CODE));
                    exchange.setProperty(CALLBACK_RETRY, 1);
                    exchange.setProperty(CALLBACK_SUCCESS, true);

                })
                .otherwise()
                .log(LoggingLevel.ERROR, "Callback request was unsuccessful")
                .process(exchange -> {
                    if (exchange.getProperty(CALLBACK_RETRY).equals(MAX_STATUS_RETRY)) {
                        logger.info("Retry Exhausted, setting Callback as Failed");
                    } else {
                        int retry = (int) exchange.getProperty(CALLBACK_RETRY);
                        retry++;
                        logger.info("Retry Left {}, Setting Callback as Failed and Retrying...",
                                ((int) exchange.getProperty(MAX_STATUS_RETRY) - retry));
                        exchange.setProperty(CALLBACK_RETRY, retry);
                        exchange.setProperty(CALLBACK_RESPONSE_CODE, exchange.getIn()
                                .getHeader(Exchange.HTTP_RESPONSE_CODE));

                    }
                    exchange.setProperty(CALLBACK_SUCCESS, false);
                    exchange.setProperty(ERROR_DESCRIPTION, exchange.getIn().getBody(String.class));
                    exchange.setProperty(ERROR_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                });

        //for temporary callback simulation
        from("rest:post:/simulate")
                .log("Reached Simulation");
    }


}
