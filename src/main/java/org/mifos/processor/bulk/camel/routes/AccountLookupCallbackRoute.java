package org.mifos.processor.bulk.camel.routes;

import io.camunda.zeebe.client.ZeebeClient;
import org.mifos.processor.bulk.schema.BatchAccountLookupResponseDTO;
import org.mifos.processor.bulk.schema.BeneficiaryDTO;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.schema.TransactionResult;
import org.mifos.processor.bulk.utility.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class AccountLookupCallbackRoute extends BaseRouteBuilder{
    @Autowired
    private ZeebeClient zeebeClient;
    private Integer totalApprovedAmount;
    private Integer totalApprovedCount;

    @Override
    public void configure() throws Exception {
        from("direct:accountLookupCallback").id("direct:accountLookupCallback").log("Starting route " + RouteId.ACCOUNT_LOOKUP_CALLBACK.name())
                .to("direct:download-file").to("direct:get-transaction-array").to("direct:batch-account-lookup-callback").process(exchange -> exchange.setProperty(OVERRIDE_HEADER, true));
        from("direct:batch-account-lookup-callback").id("direct:batch-account-lookup-callback").process(exchange -> {
                    String serverFileName = exchange.getProperty(SERVER_FILE_NAME, String.class);
                    String resultFile = String.format("Result_%s", serverFileName);
                    BatchAccountLookupResponseDTO batchAccountLookupCallback = objectMapper.readValue(exchange.getProperty("batchAccountLookupCallback", String.class), BatchAccountLookupResponseDTO.class);
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    List<TransactionResult> transactionResultList = new ArrayList<>();
                    Map<String, Object> variables = new HashMap<>();


                    updateTransactionStatus(transactionList, batchAccountLookupCallback.getBeneficiaryDTOList(), transactionResultList);
                    exchange.setProperty(PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_AMOUNT, totalApprovedAmount);
                    exchange.setProperty(PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_COUNT, totalApprovedCount);
                    exchange.setProperty(RESULT_TRANSACTION_LIST, transactionResultList);
                    exchange.setProperty(RESULT_FILE, resultFile);
                    Long workflowInstanceKey= Long.valueOf(exchange.getProperty("workflowInstanceKey").toString());
                    variables.put(PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_AMOUNT, totalApprovedAmount);
                    variables.put(PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_COUNT, totalApprovedCount);
                    if (zeebeClient != null) {

                        zeebeClient.newSetVariablesCommand(workflowInstanceKey)
                                .variables(variables)
                                .send()
                                .join();
                    }
                })
                // setting localfilepath as result file to make sure result file is uploaded
                .setProperty(LOCAL_FILE_PATH, exchangeProperty(RESULT_FILE)).setProperty(OVERRIDE_HEADER, constant(true))
                .process(exchange -> {
                    logger.info("A1 {}", exchange.getProperty(RESULT_FILE));
                    logger.info("A2 {}", exchange.getProperty(LOCAL_FILE_PATH));
                    logger.info("A3 {}", exchange.getProperty(OVERRIDE_HEADER));
                }).to("direct:update-result-file").to("direct:upload-file");
    }
    public List<TransactionResult> updateTransactionStatus(List<Transaction> transactionList, List<BeneficiaryDTO> batchAccountLookupResponseDTO, List<TransactionResult> transactionResultList) {
        totalApprovedCount = 0;
        totalApprovedAmount = 0;
        AtomicInteger count = new AtomicInteger(totalApprovedCount);
        AtomicInteger amount = new AtomicInteger(totalApprovedAmount);

        transactionList.forEach(transaction -> {
            Optional<BeneficiaryDTO> matchingBeneficiary = batchAccountLookupResponseDTO.stream()
                    .filter(beneficiary -> transaction.getPayeeIdentifier().equals(beneficiary.getPayeeIdentity()))
                    .findFirst();

            if (matchingBeneficiary.isPresent()) {
                count.incrementAndGet(); // Increment the count atomically
                try {
                    amount.addAndGet(Integer.parseInt(transaction.getAmount()));
                } catch (NumberFormatException e) {
                    logger.error(e.getMessage());
                }
                String identifier = matchingBeneficiary.get().getFinancialAddress();
                TransactionResult transactionResult = Utils.mapToResultDTO(transaction);
                transactionResult.setPayeeIdentifier(identifier);
                transactionResultList.add(transactionResult);
            } else {
                TransactionResult transactionResult = Utils.mapToResultDTO(transaction);
                transactionResult.setErrorCode("404");
                transactionResult.setErrorDescription("Payee Identifier not found");
                transactionResult.setStatus("Failed");
                transactionResultList.add(transactionResult);
            }
        });
        totalApprovedCount = count.get();
        totalApprovedAmount = amount.get();

        return transactionResultList;
    }


}
