package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_RATE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_CODE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ERROR_DESCRIPTION;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.processor.bulk.schema.BatchDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BatchAggregateRoute extends BaseRouteBuilder {

    @Value("${config.completion-threshold-check.completion-threshold}")
    private int completionThreshold;

    @Override
    public void configure() throws Exception {

        from("rest:get:test/batch/summary").to(RouteId.BATCH_AGGREGATE.getValue());

        /**
         * Base route for kicking off ordering logic. Performs below tasks. 1. Downloads the csv form cloud. 2. Builds
         * the [Transaction] array using [direct:get-transaction-array] route. 3. Format the data based on the
         * configuration provided in application.yaml. @see [Standard.java] 4. Update file with the updated data. 5.
         * Uploads the updated file in cloud.
         */
        from(RouteId.BATCH_AGGREGATE.getValue()).id(RouteId.BATCH_AGGREGATE.getValue())
                .log("Starting route " + RouteId.BATCH_AGGREGATE.name())
                .to("direct:get-access-token")
                .choice()
                .when(exchange -> exchange.getProperty(OPS_APP_ACCESS_TOKEN, String.class) != null)
                .log(LoggingLevel.INFO, "Got access token, moving on to API call")
                .to("direct:batch-aggregate-api-call")
                .to("direct:batch-aggregate-response-handler")
                .otherwise()
                .log(LoggingLevel.INFO, "Authentication failed.")
                .endChoice();

        getBaseExternalApiRequestRouteDefinition("batch-aggregate-api-call", HttpRequestMethod.GET)
//                .setHeader(Exchange.REST_HTTP_QUERY, simple("batchId=${exchangeProperty." + BATCH_ID + "}"))
                .setHeader("Authorization", simple("Bearer ${exchangeProperty." + OPS_APP_ACCESS_TOKEN + "}"))
                .setHeader(HEADER_PLATFORM_TENANT_ID, simple("${exchangeProperty." + TENANT_ID + "}")).process(exchange -> {
                    logger.info(exchange.getIn().getHeaders().toString());
                }).toD(operationsAppConfig.batchAggregateUrl + "${exchangeProperty." + BATCH_ID + "}?bridgeEndpoint=true")
                .log(LoggingLevel.DEBUG, "Batch aggregate API response: \n\n ${body}")
                .log(LoggingLevel.INFO, "Aggregate Response body: ${body}");

        from("direct:batch-aggregate-response-handler")
                .id("direct:batch-aggregate-response-handler")
                .log("Starting route direct:batch-aggregate-response-handler")
                // .setBody(exchange -> exchange.getIn().getBody(String.class))
                .choice().when(header("CamelHttpResponseCode").isEqualTo("200")).log(LoggingLevel.INFO, "Batch summary request successful")
                .unmarshal().json(JsonLibrary.Jackson, BatchDTO.class).process(exchange -> {
                    BatchDTO batchAggregateResponse = exchange.getIn().getBody(BatchDTO.class);
                    int percentage = (int) (((double) batchAggregateResponse.getSuccessful() / batchAggregateResponse.getTotal()) * 100);

                    if (percentage >= completionThreshold) {
                        logger.info("Batch success threshold reached. Expected rate: {}, Actual Rate: {}", completionThreshold, percentage);
                    }

                    exchange.setProperty(COMPLETION_RATE, percentage);

                }).otherwise().log(LoggingLevel.ERROR, "Batch aggregate request unsuccessful").process(exchange -> {
                    exchange.setProperty(BATCH_STATUS_FAILED, true);
                    exchange.setProperty(ERROR_DESCRIPTION, exchange.getIn().getBody(String.class));
                    exchange.setProperty(ERROR_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                });
    }

}
