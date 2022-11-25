package org.mifos.processor.bulk.camel.routes;



import org.apache.camel.Exchange;
import org.mifos.connector.common.gsma.dto.*;
import org.mifos.processor.bulk.camel.config.CamelProperties;
import org.mifos.processor.bulk.config.PaymentModeApiMapping;
import org.mifos.processor.bulk.config.PaymentModeConfiguration;
import org.mifos.processor.bulk.schema.TransactionResult;
import org.mifos.processor.bulk.utility.Utils;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.zeebe.BpmnConfig;
import org.mifos.processor.bulk.zeebe.ZeebeProcessStarter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;
import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.camel.config.CamelProperties.GSMA_CHANNEL_REQUEST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class InitSubBatchRoute extends BaseRouteBuilder {

    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    @Autowired
    private BpmnConfig bpmnConfig;

    @Autowired
    private PaymentModeConfiguration paymentModeConfiguration;

    @Value("${channel.hostname}")
    private String ChannelURL;


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
                .when(exchangeProperty(PAYMENT_MODE).isEqualToIgnoreCase("gsma"))
                .process(exchange -> {
                    Map<String, Object> variables = exchange.getProperty(ZEEBE_VARIABLE, Map.class);
                    variables.put(PAYMENT_MODE, "gsma");
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    exchange.setProperty("length",transactionList.size());

                })
                .loop(simple("${exchangeProperty.length}"))
                .process(exchange -> {
                    int index = exchange.getProperty(Exchange.LOOP_INDEX, Integer.class);
                    GSMATransaction gsmaTransaction =
                            convertTxnToGSMA((Transaction) exchange.getProperty(TRANSACTION_LIST, List.class)
                            .get(index));
                    exchange.setProperty(GSMA_CHANNEL_REQUEST, gsmaTransaction);
                    exchange.setProperty(INIT_SUB_BATCH_FAILED, false);
                })
                .setHeader("Platform-TenantId", exchangeProperty(TENANT_NAME))
                .setBody(exchange-> {
                    GSMATransaction gsmaTransaction = exchange.getProperty(GSMA_CHANNEL_REQUEST, GSMATransaction.class);
                    return gsmaTransaction;
                })
                .marshal().json()
                .to("direct:external-api-call") // loads the endpoint based on configuration and calls the external api
                .log("Completed start of workflow for gsma")
                .end()
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo(200))
                .process(exchange -> {
                    logger.info("reached here");
                    exchange.setProperty(INIT_SUB_BATCH_FAILED, false);})
                .otherwise()
                .process(exchange -> {
                    exchange.setProperty(INIT_SUB_BATCH_FAILED, true);
                })
                .endChoice()
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


        from("direct:external-api-call")
                .id("direct:external-api-call")
                .log("Starting route direct:external-api-call")
                .process(exchange -> {
                    String paymentMde = exchange.getProperty(PAYMENT_MODE, String.class);
                    PaymentModeApiMapping mapping = paymentModeConfiguration.getByMode(paymentMde);
                    if (mapping == null) {
                        exchange.setProperty(EXTERNAL_ENDPOINT_FAILED, true);
                    } else {
                        exchange.setProperty(EXTERNAL_ENDPOINT_FAILED, false);
                        exchange.setProperty(EXTERNAL_ENDPOINT, mapping.getEndpoint());
                    }
                })
                .choice()
                .when(exchangeProperty(EXTERNAL_ENDPOINT_FAILED).isEqualTo(false))
                .toD(ChannelURL + "${exchangeProperty.EXTERNAL_ENDPOINT}" + "?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .otherwise()
                .endChoice();

    }

    private GSMATransaction convertTxnToGSMA(Transaction transaction) {
        GSMATransaction gsmaTransaction = new GSMATransaction();
        gsmaTransaction.setAmount(transaction.getAmount());
        gsmaTransaction.setCurrency(transaction.getCurrency());
        GsmaParty payer = new GsmaParty();
        //logger.info("Payer {} {}", transaction.getPayerIdentifier(),payer[0].);
        payer.setKey("msisdn");
        payer.setValue(transaction.getPayerIdentifier());
        GsmaParty payee = new GsmaParty();
        payee.setKey("msisdn");
        payee.setValue(transaction.getPayeeIdentifier());
        GsmaParty[] debitParty = new GsmaParty[1];
        GsmaParty[] creditParty = new GsmaParty[1];
        debitParty[0] = payer;
        creditParty[0] = payee;
        gsmaTransaction.setDebitParty(debitParty);
        gsmaTransaction.setCreditParty(creditParty);
        gsmaTransaction.setRequestingOrganisationTransactionReference("string");
        gsmaTransaction.setSubType("string");
        gsmaTransaction.setDescriptionText("string");
        Fee fees = new Fee();
        fees.setFeeType(transaction.getAmount());
        fees.setFeeCurrency(transaction.getCurrency());
        fees.setFeeType("string");
        Fee[] fee = new Fee[1];
        fee[0] = fees;
        gsmaTransaction.setFees(fee);
        gsmaTransaction.setGeoCode("37.423825,-122.082900");
        InternationalTransferInformation internationalTransferInformation =
                new InternationalTransferInformation();
        internationalTransferInformation.setQuotationReference("string");
        internationalTransferInformation.setQuoteId("string");
        internationalTransferInformation.setDeliveryMethod("directtoaccount");
        internationalTransferInformation.setOriginCountry("USA");
        internationalTransferInformation.setReceivingCountry("USA");
        internationalTransferInformation.setRelationshipSender("string");
        internationalTransferInformation.setRemittancePurpose("string");
        gsmaTransaction.setInternationalTransferInformation(internationalTransferInformation);
        gsmaTransaction.setOneTimeCode("string");
        IdDocument idDocument = new IdDocument();
        idDocument.setIdType("passport");
        idDocument.setIdNumber("string");
        idDocument.setIssuerCountry("USA");
        idDocument.setExpiryDate("2022-09-28T12:51:19.260+00:00");
        idDocument.setIssueDate("2022-09-28T12:51:19.260+00:00");
        idDocument.setIssuer("string");
        idDocument.setIssuerPlace("string");
        IdDocument[] idDocuments = new IdDocument[1];
        idDocuments[0] = idDocument;
        PostalAddress postalAddress = new PostalAddress();
        postalAddress.setAddressLine1("string");
        postalAddress.setAddressLine2("string");
        postalAddress.setAddressLine3("string");
        postalAddress.setCity("string");
        postalAddress.setCountry("USA");
        postalAddress.setPostalCode("string");
        postalAddress.setStateProvince("string");
        SubjectName subjectName = new SubjectName();
        subjectName.setFirstName("string");
        subjectName.setLastName("string");
        subjectName.setMiddleName("string");
        subjectName.setTitle("string");
        subjectName.setNativeName("string");
        Kyc recieverKyc = new Kyc();
        recieverKyc.setBirthCountry("USA");
        recieverKyc.setDateOfBirth("2000-11-20");
        recieverKyc.setContactPhone("string");
        recieverKyc.setEmailAddress("string");
        recieverKyc.setEmployerName("string");
        recieverKyc.setGender('m');
        recieverKyc.setIdDocument(idDocuments);
        recieverKyc.setNationality("USA");
        recieverKyc.setOccupation("string");
        recieverKyc.setPostalAddress(postalAddress);
        recieverKyc.setSubjectName(subjectName);
        Kyc senderKyc = new Kyc();
        senderKyc.setBirthCountry("USA");
        senderKyc.setDateOfBirth("2000-11-20");
        senderKyc.setContactPhone("string");
        senderKyc.setEmailAddress("string");
        senderKyc.setEmployerName("string");
        senderKyc.setGender('m');
        senderKyc.setIdDocument(idDocuments);
        senderKyc.setNationality("USA");
        senderKyc.setOccupation("string");
        senderKyc.setPostalAddress(postalAddress);
        senderKyc.setSubjectName(subjectName);
        gsmaTransaction.setReceiverKyc(recieverKyc);
        gsmaTransaction.setSenderKyc(senderKyc);
        gsmaTransaction.setServicingIdentity("string");
        gsmaTransaction.setRequestDate("2022-09-28T12:51:19.260+00:00");


        return gsmaTransaction;
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
