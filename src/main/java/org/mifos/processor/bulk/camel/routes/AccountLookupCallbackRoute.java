package org.mifos.processor.bulk.camel.routes;

import com.amazonaws.services.dynamodbv2.xspec.L;
import org.apache.camel.Exchange;
import org.mifos.connector.common.identityaccountmapper.dto.AccountMapperRequestDTO;
import org.mifos.processor.bulk.schema.BatchAccountLookupResponseDTO;
import org.mifos.processor.bulk.schema.BeneficiaryDTO;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.schema.TransactionResult;
import org.mifos.processor.bulk.utility.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REGISTERING_INSTITUTION_ID;

public class AccountLookupCallbackRoute extends BaseRouteBuilder{

    @Override
    public void configure() throws Exception {
        from(RouteId.ACCOUNT_LOOKUP_CALLBACK.getValue()).id(RouteId.ACCOUNT_LOOKUP_CALLBACK.getValue()).log("Starting route " + RouteId.ACCOUNT_LOOKUP_CALLBACK.name())
                .to("direct:download-file").to("direct:get-transaction-array").to("direct:batch-account-lookup-callback").to("direct:update-file")
                .to("direct:upload-file").process(exchange -> {
                    exchange.setProperty(PARTY_LOOKUP_FAILED, false);
                });
        from("direct:batch-account-lookup-callback").id("direct:batch-account-lookup-callback").process(exchange -> {
                    String serverFileName = exchange.getProperty(SERVER_FILE_NAME, String.class);
                    String resultFile = String.format("Result_%s", serverFileName);
                    BatchAccountLookupResponseDTO batchAccountLookupCallback = objectMapper.readValue(exchange.getProperty("batchAccountLookupCallback", String.class), BatchAccountLookupResponseDTO.class);
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    List<TransactionResult> transactionResultList = new ArrayList<>();
                    Integer totalApprovedAmount = 0;
                    Integer totalApprovedCount = 0;
                    updateTransactionStatus(transactionList, batchAccountLookupCallback.getBeneficiaryDTOList(), transactionResultList, totalApprovedCount, totalApprovedAmount);
                    exchange.setProperty(PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_AMOUNT, totalApprovedAmount);
                    exchange.setProperty(PARTY_LOOKUP_SUCCESSFUL_TRANSACTION_COUNT, totalApprovedCount);
                    exchange.setProperty(RESULT_TRANSACTION_LIST, transactionResultList);
                    exchange.setProperty(RESULT_FILE, resultFile);
                })
                // setting localfilepath as result file to make sure result file is uploaded
                .setProperty(LOCAL_FILE_PATH, exchangeProperty(RESULT_FILE)).setProperty(OVERRIDE_HEADER, constant(true))
                .process(exchange -> {
                    logger.info("A1 {}", exchange.getProperty(RESULT_FILE));
                    logger.info("A2 {}", exchange.getProperty(LOCAL_FILE_PATH));
                    logger.info("A3 {}", exchange.getProperty(OVERRIDE_HEADER));
                }).to("update-transaction-status").to("direct:update-result-file").to("direct:upload-file");
    }
    public List<TransactionResult> updateTransactionStatus(List<Transaction> transactionList, List<BeneficiaryDTO> batchAccountLookupResponseDTO, List<TransactionResult> transactionResultList,
                                                           Integer totalApprovedCount, Integer totalApprovedAmount){
        transactionResultList.add(new TransactionResult());
        for (Transaction transaction : transactionList) {
            boolean found = false;

            for (BeneficiaryDTO beneficiary : batchAccountLookupResponseDTO) {
                if (transaction.getPayeeIdentifier().equals(beneficiary.getPayeeIdentity())) {
                    found = true;
                    totalApprovedCount++;
                    try {
                        totalApprovedAmount += Integer.parseInt(transaction.getAmount());
                    } catch (NumberFormatException e) {
                        logger.error(e.getMessage());
                    }
                    break;
                }
            }

            if (!found) {
                TransactionResult transactionResult = Utils.mapToResultDTO(transaction);
                transactionResult.setErrorCode("404");
                transactionResult.setErrorDescription("Payee Identifier not found");
                transactionResult.setStatus("Failed");
                transactionResultList.add(transactionResult);
            }
        }
        return transactionResultList;
    }
}
