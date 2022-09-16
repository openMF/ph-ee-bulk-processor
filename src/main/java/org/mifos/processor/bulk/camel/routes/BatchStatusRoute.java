package org.mifos.processor.bulk.camel.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.processor.bulk.schema.BatchDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import static org.mifos.processor.bulk.camel.config.CamelProperties.BATCH_STATUS_FAILED;
import static org.mifos.processor.bulk.camel.config.CamelProperties.OPS_APP_ACCESS_TOKEN;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class BatchStatusRoute extends BaseRouteBuilder {

    @Value("${config.success-threshold-check.success-threshold}")
    private int successThreshold;

    @Override
    public void configure() throws Exception {

        from("rest:get:test/batch/summary")
                .to(RouteId.BATCH_STATUS.getValue());

        /**
         * Base route for kicking off ordering logic. Performs below tasks.
         * 1. Downloads the csv form cloud.
         * 2. Builds the [Transaction] array using [direct:get-transaction-array] route.
         * 3. Format the data based on the configuration provided in application.yaml. @see [Standard.java]
         * 4. Update file with the updated data.
         * 5. Uploads the updated file in cloud.
         */
        from(RouteId.BATCH_STATUS.getValue())
                .id(RouteId.BATCH_STATUS.getValue())
                .log("Starting route " + RouteId.BATCH_STATUS.name())
                .to("direct:get-access-token")
                .choice()
                .when(exchange -> exchange.getProperty(OPS_APP_ACCESS_TOKEN, String.class) != null)
                .log(LoggingLevel.INFO, "Got access token, moving on to API call")
                .to("direct:batch-summary")
                .to("direct:batch-summary-response-handler")
                .otherwise()
                .log(LoggingLevel.INFO, "Authentication failed.")
                .endChoice();


        getBaseExternalApiRequestRouteDefinition("batch-summary", HttpRequestMethod.GET)
                .setHeader(Exchange.REST_HTTP_QUERY, simple("batchId=${exchangeProperty." + BATCH_ID + "}"))
                .setHeader("Authorization", simple("Bearer ${exchangeProperty."+OPS_APP_ACCESS_TOKEN+"}"))
                .setHeader("Platform-TenantId", simple("${exchangeProperty." + TENANT_ID + "}"))
                .process(exchange -> {
                    logger.info(exchange.getIn().getHeaders().toString());
                })
                .toD(operationsAppConfig.batchSummaryUrl + "?bridgeEndpoint=true")
                .log(LoggingLevel.INFO, "Batch summary API response: \n\n ${body}");

        from("direct:batch-summary-response-handler")
                .id("direct:batch-summary-response-handler")
                .log("Starting route direct:batch-summary-response-handler")
                //.setBody(exchange -> exchange.getIn().getBody(String.class))
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log(LoggingLevel.INFO, "Batch summary request successful")
                .unmarshal().json(JsonLibrary.Jackson, BatchDTO.class)
                .process(exchange -> {
                    BatchDTO batchSummary = exchange.getIn().getBody(BatchDTO.class);

                    long percentage = (batchSummary.getSuccessful()/batchSummary.getTotal())*100;

                    if (percentage >= successThreshold) {
                        logger.info("Batch success threshold reached. Expected rate: {}, Actual Rate: {}",
                                successThreshold, percentage);
                    }

                    exchange.setProperty(SUCCESS_RATE, percentage);

                })
                .otherwise()
                .log(LoggingLevel.ERROR, "Batch summary request unsuccessful")
                .process(exchange -> {
                    exchange.setProperty(BATCH_STATUS_FAILED, true);
                    exchange.setProperty(ERROR_DESCRIPTION, exchange.getIn().getBody(String.class));
                    exchange.setProperty(ERROR_CODE, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
                });
    }

}
