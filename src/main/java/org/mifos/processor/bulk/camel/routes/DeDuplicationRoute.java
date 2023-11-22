package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.DUPLICATE_TRANSACTION_LIST;
import static org.mifos.processor.bulk.camel.config.CamelProperties.LOCAL_FILE_PATH;
import static org.mifos.processor.bulk.camel.config.CamelProperties.ORIGINAL_TRANSACTION_LIST;
import static org.mifos.processor.bulk.camel.config.CamelProperties.OVERRIDE_HEADER;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.DE_DUPLICATION_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.DUPLICATE_TRANSACTION_COUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FAILED_TRANSACTION_FILE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.stereotype.Component;

@Component
public class DeDuplicationRoute extends BaseRouteBuilder {

    @Override
    public void configure() throws Exception {
        from(RouteId.DE_DUPLICATION.getValue()).id(RouteId.DE_DUPLICATION.getValue())
                .log("Started route " + RouteId.DE_DUPLICATION.getValue()).to("direct:download-file").to("direct:get-transaction-array")
                .process(exchange -> {
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);

                    if (Objects.isNull(transactionList) || transactionList.isEmpty()) {
                        exchange.setProperty(DE_DUPLICATION_FAILED, false);
                        exchange.setProperty(DUPLICATE_TRANSACTION_COUNT, 0);
                    }

                    int duplicateTxnCount = 0;
                    List<Transaction> duplicateTransactionList = new ArrayList<>(); // contains the duplicate
                                                                                    // transaction
                    List<Transaction> originalTransactionList = new ArrayList<>(); // contains the original txn after
                                                                                   // removing duplicate
                    Set<String> set = new HashSet<>();

                    for (Transaction transaction : transactionList) {
                        String payeeDetail = fetchPayeeDetail(transaction);
                        if (set.contains(payeeDetail)) {
                            transaction.setNote("Duplicate transaction.");
                            duplicateTransactionList.add(transaction);
                            duplicateTxnCount++;
                        } else {
                            set.add(payeeDetail);
                            originalTransactionList.add(transaction);
                        }
                    }

                    log.info("Duplicate txn: {} and count: {}", duplicateTransactionList, duplicateTxnCount);

                    exchange.setProperty(DUPLICATE_TRANSACTION_COUNT, duplicateTxnCount);
                    exchange.setProperty(DUPLICATE_TRANSACTION_LIST, duplicateTransactionList);
                    exchange.setProperty(ORIGINAL_TRANSACTION_LIST, originalTransactionList);
                }).choice().when(exchange -> exchange.getProperty(DUPLICATE_TRANSACTION_COUNT, Integer.class) > 0)
                .log("Updating original transaction list")
                .setProperty(TRANSACTION_LIST, simple("${exchangeProperty." + ORIGINAL_TRANSACTION_LIST + "}"))
                .setProperty(LOCAL_FILE_PATH, simple("${exchangeProperty." + SERVER_FILE_NAME + "}"))
                .setProperty(OVERRIDE_HEADER, constant(true)).to("direct:update-file").to("direct:upload-file").process(exchange -> {
                    String originalFileServerName = exchange.getProperty(SERVER_FILE_NAME, String.class);
                    String duplicateFileName = "duplicate_transaction_" + originalFileServerName;

                    exchange.setProperty(FAILED_TRANSACTION_FILE, duplicateFileName);
                }).log("Updating duplicate transaction list")
                .setProperty(TRANSACTION_LIST, simple("${exchangeProperty." + DUPLICATE_TRANSACTION_LIST + "}"))
                .setProperty(LOCAL_FILE_PATH, simple("${exchangeProperty." + FAILED_TRANSACTION_FILE + "}"))
                .setProperty(OVERRIDE_HEADER, constant(true)).to("direct:update-file").to("direct:upload-file").process(exchange -> {
                    // checking if file upload was success or
                    String serverFileName = exchange.getProperty(SERVER_FILE_NAME, String.class);
                    if (serverFileName == null) {
                        exchange.setProperty(DE_DUPLICATION_FAILED, true);
                    } else {
                        exchange.setProperty(DE_DUPLICATION_FAILED, false);
                    }
                }).otherwise().log("No duplicate transaction found").setProperty(DE_DUPLICATION_FAILED, constant(false)).endChoice();
    }

    private void removeDuplicatesIfOrderingDisabled(List<Transaction> transactionList) {
        Set<String> set = new HashSet<>();

        if (Objects.isNull(transactionList)) {
            return;
        }

        for (Transaction transaction : transactionList) {
            String payeeDetail = fetchPayeeDetail(transaction);
            if (set.contains(payeeDetail)) {
                transaction.setNote("Duplicate transaction.");
            } else {
                set.add(payeeDetail);
            }
        }
    }

    private Map<String, Transaction> getTransactionPayeeDetailHashMap(List<Transaction> transactionList) {
        Map<String, Transaction> payeeDetailTransactionMap = new HashMap<>();
        for (Transaction transaction : transactionList) {
            payeeDetailTransactionMap.put(fetchPayeeDetail(transaction), transaction);
        }
        return payeeDetailTransactionMap;
    }

    private String fetchPayeeDetail(Transaction transaction) {
        String payeeIdentifier = transaction.getPayeeIdentifier();
        String payeeIdentifierType = transaction.getPayeeIdentifierType();
        String amount = transaction.getAmount();
        String currency = transaction.getCurrency();

        return String.format("%s%s%s%s", payeeIdentifier, payeeIdentifierType, amount, currency);
    }

}
