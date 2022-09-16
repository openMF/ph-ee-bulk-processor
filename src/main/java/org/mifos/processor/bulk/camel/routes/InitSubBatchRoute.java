package org.mifos.processor.bulk.camel.routes;

import org.mifos.processor.bulk.utility.Utils;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.zeebe.BpmnConfig;
import org.mifos.processor.bulk.zeebe.ZeebeProcessStarter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class InitSubBatchRoute extends BaseRouteBuilder {

    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    @Autowired
    private BpmnConfig bpmnConfig;

    @Override
    public void configure() throws Exception {

        /**
         * Base route for kicking off init sub batch logic. Performs below tasks.
         * 1. Downloads the csv form cloud.
         * 2. Builds the [Transaction] array using [direct:get-transaction-array] route.
         * 3. Loops through each transaction and start the respective workflow
         */
        from(RouteId.INIT_SUB_BATCH.getValue())
                .id(RouteId.INIT_SUB_BATCH.getValue())
                .log("Starting route " + RouteId.INIT_SUB_BATCH.name())
                .to("direct:download-file")
                .to("direct:get-transaction-array")
                .to("direct:start-workflow");

        // Loops through each transaction and start the respective workflow
        from("direct:start-workflow")
                .id("direct:start-flow")
                .log("Starting route direct:start-flow")
                .process(exchange -> {
                    String tenantName = exchange.getProperty(TENANT_NAME, String.class);
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    if (transactionList.get(0).getPaymentMode().equalsIgnoreCase("slcb")) {
                        Map<String, Object> variables = new HashMap<>();
                        variables.put(BATCH_ID, exchange.getProperty(BATCH_ID));
                        variables.put(SUB_BATCH_ID, UUID.randomUUID().toString());
                        variables.put(FILE_NAME, exchange.getProperty(SERVER_FILE_NAME));
                        variables.put(REQUEST_ID, exchange.getProperty(REQUEST_ID));
                        variables.put(PURPOSE, exchange.getProperty(PURPOSE));
                        variables.put(TOTAL_AMOUNT, exchange.getProperty(TOTAL_AMOUNT));
                        variables.put(ONGOING_AMOUNT, exchange.getProperty(ONGOING_AMOUNT));
                        variables.put(FAILED_AMOUNT, exchange.getProperty(FAILED_AMOUNT));
                        variables.put(COMPLETED_AMOUNT, exchange.getProperty(COMPLETED_AMOUNT));
                        variables.put(RESULT_FILE, String.format("Result_%s",
                                exchange.getProperty(SERVER_FILE_NAME)));
                        variables.put(PAYMENT_MODE, "slcb");

                        zeebeProcessStarter.startZeebeWorkflow(
                                Utils.getTenantSpecificWorkflowId(bpmnConfig.slcbBpmn, tenantName), variables);
                    }

                    exchange.setProperty(INIT_SUB_BATCH_FAILED, false);
                });
    }

}
