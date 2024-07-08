package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.BATCH_ID_HEADER;
import static org.mifos.processor.bulk.camel.config.CamelProperties.EXTERNAL_ENDPOINT;
import static org.mifos.processor.bulk.camel.config.CamelProperties.EXTERNAL_ENDPOINT_FAILED;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.IS_PAYMENT_MODE_VALID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.LOCAL_FILE_PATH;
import static org.mifos.processor.bulk.camel.config.CamelProperties.OVERRIDE_HEADER;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PAYMENT_MODE_TYPE;
import static org.mifos.processor.bulk.camel.config.CamelProperties.RESULT_TRANSACTION_LIST;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SUB_BATCH_ENTITY;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TENANT_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST_ELEMENT;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST_LENGTH;
import static org.mifos.processor.bulk.camel.config.CamelProperties.ZEEBE_VARIABLE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETED_AMOUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.DEBULKINGDFSPID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FAILED_AMOUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.INIT_SUB_BATCH_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ONGOING_AMOUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_LOOKUP_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYEE_DFSP_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYMENT_MODE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.RESULT_FILE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TOTAL_AMOUNT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.mifos.processor.bulk.config.ExternalApiPayloadConfig;
import org.mifos.processor.bulk.config.PaymentModeConfiguration;
import org.mifos.processor.bulk.config.PaymentModeMapping;
import org.mifos.processor.bulk.config.PaymentModeType;
import org.mifos.processor.bulk.schema.SubBatchEntity;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.schema.TransactionResult;
import org.mifos.processor.bulk.utility.Utils;
import org.mifos.processor.bulk.zeebe.BpmnConfig;
import org.mifos.processor.bulk.zeebe.ZeebeProcessStarter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InitSubBatchRoute extends BaseRouteBuilder {

    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    @Autowired
    private BpmnConfig bpmnConfig;

    @Autowired
    private PaymentModeConfiguration paymentModeConfiguration;

    @Autowired
    private ExternalApiPayloadConfig externalApiPayloadConfig;

    @Value("${channel.hostname}")
    private String channelURL;

    @Value("${config.partylookup.enable}")
    private boolean isPartyLookupEnabled;

    @Override
    public void configure() throws Exception {

        /**
         * Base route for kicking off init sub batch logic. Performs below tasks. 1. Downloads the csv form cloud. 2.
         * Builds the [Transaction] array using [direct:get-transaction-array] route. 3. Loops through each transaction
         * and start the respective workflow
         */
        from(RouteId.INIT_SUB_BATCH.getValue()).id(RouteId.INIT_SUB_BATCH.getValue()).log("Starting route " + RouteId.INIT_SUB_BATCH.name())
                .to("direct:download-file").to("direct:get-transaction-array").to("direct:start-workflow-step1");

        // crates the zeebe variables map and starts the workflow by calling >> direct:start-workflow-step2
        from("direct:start-workflow-step1").id("direct:start-flow-step1").log("Starting route direct:start-flow-step1")
                .process(exchange -> {
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);

                    Map<String, Object> variables = exchange.getProperty(ZEEBE_VARIABLE, Map.class);
                    variables.put(BATCH_ID, exchange.getProperty(BATCH_ID));
                    variables.put(FILE_NAME, exchange.getProperty(SERVER_FILE_NAME));
                    variables.put(REQUEST_ID, exchange.getProperty(REQUEST_ID));
                    variables.put(PURPOSE, exchange.getProperty(PURPOSE));
                    variables.put(TOTAL_AMOUNT, exchange.getProperty(TOTAL_AMOUNT));
                    variables.put(ONGOING_AMOUNT, exchange.getProperty(ONGOING_AMOUNT));
                    variables.put(FAILED_AMOUNT, exchange.getProperty(FAILED_AMOUNT));
                    variables.put(COMPLETED_AMOUNT, exchange.getProperty(COMPLETED_AMOUNT));
                    variables.put(RESULT_FILE, String.format("Result_%s", exchange.getProperty(SERVER_FILE_NAME)));

                    exchange.setProperty(ZEEBE_VARIABLE, variables);
                    exchange.setProperty(PAYMENT_MODE, transactionList.get(0).getPaymentMode());

                }).to("direct:start-workflow-step2");

        from("direct:start-workflow-step2").id("direct:start-flow-step2").log("Starting route direct:start-flow-step2")
                .to("direct:validate-payment-mode").choice()
                // if invalid payment mode
                .when(exchangeProperty(IS_PAYMENT_MODE_VALID).isEqualTo(false)).to("direct:payment-mode-missing")
                .setProperty(INIT_SUB_BATCH_FAILED, constant(true))
                // else
                .otherwise().to("direct:start-workflow-step3").endChoice();

        from("direct:start-workflow-step3").id("direct:start-flow-step3").log("Starting route direct:start-flow-step3").choice()
                // if type of payment mode is bulk
                .when(exchangeProperty(PAYMENT_MODE_TYPE).isEqualTo(PaymentModeType.BULK)).process(exchange -> {
                    String paymentMode = exchange.getProperty(PAYMENT_MODE, String.class);
                    PaymentModeMapping mapping = paymentModeConfiguration.getByMode(paymentMode);

                    String tenantName = exchange.getProperty(TENANT_NAME, String.class);
                    Map<String, Object> variables = exchange.getProperty(ZEEBE_VARIABLE, Map.class);
                    variables.put(PAYMENT_MODE, paymentMode);
                    variables.put(DEBULKINGDFSPID, mapping.getDebulkingDfspid() == null ? tenantName : mapping.getDebulkingDfspid());
                    if (isPartyLookupEnabled && !(Boolean) variables.get(PARTY_LOOKUP_FAILED)) {
                        String filename = exchange.getProperty(SERVER_FILE_NAME).toString();
                        String regex = ".*_sub-batch-([\\w-]+)\\.csv"; //payee DFSP Id for sub batch are extracted from the sub batch file name when party lookup is enabled and it is successful
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(filename);

                        if (matcher.matches()) {
                            String payeeDfspId = matcher.group(1);
                            logger.debug("Payee DFSP Id {}", payeeDfspId);
                            variables.put(PAYEE_DFSP_ID, payeeDfspId);
                        }
                    }
                    zeebeProcessStarter.startZeebeWorkflow(
                            Utils.getBulkConnectorBpmnName(mapping.getEndpoint(), mapping.getId().toLowerCase(), tenantName), variables);
                    exchange.setProperty(INIT_SUB_BATCH_FAILED, false);
                })
                // if type of payment mode is payment todo // else case or else if case ?
                .otherwise().loop(simple("${exchangeProperty." + TRANSACTION_LIST_LENGTH + "}")).process(exchange -> {
                    int index = exchange.getProperty(Exchange.LOOP_INDEX, Integer.class);
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    Transaction transaction = transactionList.get(index);

                    exchange.setProperty(REQUEST_ID, transaction.getRequestId());
                    exchange.setProperty(PAYEE_DFSP_ID, transaction.getPayeeDfspId());
                    logger.info("REQUEST_ID: {}", transaction.getRequestId());
                    exchange.setProperty(TRANSACTION_LIST_ELEMENT, transaction);
                }).setHeader("Platform-TenantId", exchangeProperty(TENANT_NAME))
                .setHeader("X-PayeeDFSP-ID", exchangeProperty(PAYEE_DFSP_ID)).to("direct:dynamic-payload-setter")
                .to("direct:external-api-call").to("direct:external-api-response-handler").end() // end loop block
                .endChoice();

        from("direct:dynamic-payload-setter").id("direct:runtime-payload-test").log("Starting route direct:runtime-payload-test")
                .process(exchange -> {
                    String mode = exchange.getProperty(PAYMENT_MODE, String.class);
                    Function<Exchange, String> localPayloadVariable = externalApiPayloadConfig.getApiPayloadSetter(mode);
                    logger.info("MODE FOR API CALL : {}", mode);
                    logger.info("localPayloadVariable: {}", localPayloadVariable);
                    exchange.setProperty("body", localPayloadVariable.apply(exchange));
                })
                // this payload variable returns the body for respective payment modes
                .setBody(simple("${exchangeProperty.body}"));

        // Loops through each transaction and start the respective workflow
        from("direct:external-api-response-handler").id("direct:external-api-response-handler")
                .log("Starting route direct:external-api-response-handler").choice().when(header("CamelHttpResponseCode").isEqualTo(200))
                .process(exchange -> {
                    logger.info("INIT_SUB_BATCH_FAILED is false");
                    exchange.setProperty(INIT_SUB_BATCH_FAILED, false);
                }).otherwise().process(exchange -> {
                    logger.info("INIT_SUB_BATCH_FAILED is false");
                    exchange.setProperty(INIT_SUB_BATCH_FAILED, true);
                }).endChoice();

        from("direct:payment-mode-missing").id("direct:payment-mode-missing").log("Starting route direct:payment-mode-missing")
                .process(exchange -> {
                    String serverFileName = exchange.getProperty(SERVER_FILE_NAME, String.class);
                    String resultFile = String.format("Result_%s", serverFileName);

                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    List<TransactionResult> transactionResultList = updateTransactionStatusToFailed(transactionList);
                    exchange.setProperty(RESULT_TRANSACTION_LIST, transactionResultList);
                    exchange.setProperty(RESULT_FILE, resultFile);
                })
                // setting localfilepath as result file to make sure result file is uploaded
                .setProperty(LOCAL_FILE_PATH, exchangeProperty(RESULT_FILE)).setProperty(OVERRIDE_HEADER, constant(true))
                .process(exchange -> {
                    logger.info("A1 {}", exchange.getProperty(RESULT_FILE));
                    logger.info("A2 {}", exchange.getProperty(LOCAL_FILE_PATH));
                    logger.info("A3 {}", exchange.getProperty(OVERRIDE_HEADER));
                }).to("direct:update-result-file").to("direct:upload-file");

        from("direct:external-api-call").id("direct:external-api-call").log("Starting route direct:external-api-call").process(exchange -> {
            String paymentMode = exchange.getProperty(PAYMENT_MODE, String.class);
            PaymentModeMapping mapping = paymentModeConfiguration.getByMode(paymentMode);
            if (mapping == null) {
                exchange.setProperty(EXTERNAL_ENDPOINT_FAILED, true);
                logger.info("Failed to get the payment mode config, check the configuration for payment mode");
            } else {
                exchange.setProperty(EXTERNAL_ENDPOINT_FAILED, false);
                exchange.setProperty(EXTERNAL_ENDPOINT, mapping.getEndpoint());
                logger.info("Got the config with routing to endpoint {}", mapping.getEndpoint());
            }
        }).choice().when(exchangeProperty(EXTERNAL_ENDPOINT_FAILED).isEqualTo(false))
                .log(LoggingLevel.DEBUG, "Making API call to endpoint ${exchangeProperty.extEndpoint} and body: ${body}")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json")).choice()
                .when(exchange -> exchange.getProperty(SUB_BATCH_ENTITY, SubBatchEntity.class) != null)
                .log("Sub batch entity is not null, hence passing subBatchId while calling channel API").process(exchange -> {
                    SubBatchEntity subBatchEntity = exchange.getProperty(SUB_BATCH_ENTITY, SubBatchEntity.class);
                    exchange.getIn().setHeader(BATCH_ID_HEADER, subBatchEntity.getSubBatchId());
                }).otherwise().log("Sub batch entity is null, hence passing batchId while calling channel API")
                .setHeader(BATCH_ID_HEADER, simple("${exchangeProperty." + BATCH_ID + "}")).endChoice()
                .setHeader(HEADER_CLIENT_CORRELATION_ID, simple("${exchangeProperty." + REQUEST_ID + "}"))
                .setHeader(HEADER_REGISTERING_INSTITUTE_ID, simple("${exchangeProperty." + HEADER_REGISTERING_INSTITUTE_ID + "}"))
                .process(exchange -> {
                    log.debug("Variables: {}", exchange.getProperties());
                    log.debug("Emergency: {}", exchange.getIn().getHeaders());
                })

                .toD(channelURL + "${exchangeProperty.extEndpoint}" + "?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log(LoggingLevel.DEBUG, "Response body: ${body}").otherwise().endChoice();

        from("direct:validate-payment-mode").id("direct:validate-payment-mode").log("Starting route direct:validate-payment-mode")
                .process(exchange -> {
                    String paymentMde = exchange.getProperty(PAYMENT_MODE, String.class);
                    PaymentModeMapping mapping = paymentModeConfiguration.getByMode(paymentMde);
                    if (mapping == null) {
                        exchange.setProperty(IS_PAYMENT_MODE_VALID, false);
                    } else {
                        exchange.setProperty(IS_PAYMENT_MODE_VALID, true);
                        exchange.setProperty(PAYMENT_MODE_TYPE, mapping.getType());
                    }
                });
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
