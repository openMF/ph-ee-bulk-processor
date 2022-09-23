package org.mifos.processor.bulk.camel.routes;

import org.mifos.processor.bulk.schema.TransactionResult;
import org.mifos.processor.bulk.utility.Utils;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.zeebe.BpmnConfig;
import org.mifos.processor.bulk.zeebe.ZeebeProcessStarter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

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
                .to("direct:start-workflow-step1");

        // crates the zeebe variables map and starts the workflow by calling >> direct:start-workflow-step2
        from("direct:start-workflow-step1")
                .id("direct:start-flow-step1")
                .log("Starting route direct:start-flow-step1")
                .process(exchange -> {
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);

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

                    exchange.setProperty(ZEEBE_VARIABLE, variables);
                    exchange.setProperty(PAYMENT_MODE, transactionList.get(0).getPaymentMode());

                })
                .to("direct:start-workflow-step2");

        // Loops through each transaction and start the respective workflow
        from("direct:start-workflow-step2")
                .id("direct:start-flow-step2")
                .log("Starting route direct:start-flow-step2")
                .choice()
                .when(exchangeProperty(PAYMENT_MODE).isEqualToIgnoreCase("slcb"))
                .process(exchange -> {
                    String tenantName = exchange.getProperty(TENANT_NAME, String.class);
                    Map<String, Object> variables = exchange.getProperty(ZEEBE_VARIABLE, Map.class);
                    variables.put(PAYMENT_MODE, "slcb");
                    zeebeProcessStarter.startZeebeWorkflow(
                            Utils.getTenantSpecificWorkflowId(bpmnConfig.slcbBpmn, tenantName), variables);
                    exchange.setProperty(INIT_SUB_BATCH_FAILED, false);
                })
                .otherwise()
                .to("direct:payment-mode-missing")
                .setProperty(INIT_SUB_BATCH_FAILED, constant(true))
                .endChoice();

        from("direct:payment-mode-missing")
                .id("direct:payment-mode-missing")
                .log("Starting route direct:payment-mode-missing")
                .process(exchange -> {
                    String serverFileName = exchange.getProperty(SERVER_FILE_NAME, String.class);
                    String resultFile = String.format("Result_%s", serverFileName);

                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    List<TransactionResult> transactionResultList = updateTransactionStatusToFailed(transactionList);
                    exchange.setProperty(RESULT_TRANSACTION_LIST, transactionResultList);
                    exchange.setProperty(RESULT_FILE, resultFile);
                })
                // setting localfilpath as result file to make sure result file is uploaded
                .setProperty(LOCAL_FILE_PATH, exchangeProperty(RESULT_FILE))
                .setProperty(OVERRIDE_HEADER, constant(true))
                .process(exchange -> {
                    logger.info("A1 {}", exchange.getProperty(RESULT_FILE));
                    logger.info("A2 {}", exchange.getProperty(LOCAL_FILE_PATH));
                    logger.info("A3 {}", exchange.getProperty(OVERRIDE_HEADER));
                })
                .to("direct:update-result-file")
                .to("direct:upload-file");
    }

    // update Transactions status to failed
    private List<TransactionResult> updateTransactionStatusToFailed(List<Transaction> transactionList) {
        List<TransactionResult> transactionResultList = new ArrayList<>();
        for (Transaction transaction : transactionList) {
            TransactionResult transactionResult = Utils.mapToResultDTO(transaction);
            transactionResult.setErrorCode("404");
            transactionResult.setErrorDescription("Payment mode not configured");
            transactionResult.setStatus("Failed");
            transactionResultList.add(transactionResult);
        }

        return transactionResultList;
    }

}
