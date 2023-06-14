package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ORDERED_BY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ORDERING_FAILED;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.stereotype.Component;

@Component
public class OrderingWorker extends BaseWorker {

    @Override
    public void setup() {

        /**
         * This worker is responsible for ordering the data set based on field configuration. Performs below tasks. 1.
         * Downloads the file from cloud. 2. Parse the data into POJO. 3. Re-order the data based on field configured in
         * application.yaml 4. Uploads the updated file in cloud
         */
        newWorker(Worker.ORDERING, (client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();
            Exchange exchange = new DefaultExchange(camelContext);

            if (workerConfig.isOrderingWorkerEnabled) {
                variables.put(ORDERING_FAILED, false);
                String filename = (String) variables.get(FILE_NAME);
                exchange.setProperty(SERVER_FILE_NAME, filename);

                try {
                    sendToCamelRoute(RouteId.ORDERING, exchange);
                    assert !exchange.getProperty(ORDERING_FAILED, Boolean.class);
                } catch (Exception e) {
                    variables.put(ORDERING_FAILED, true);
                }
                variables.put(ORDERING_FAILED, false);
                variables.put(ORDERED_BY, exchange.getProperty(ORDERED_BY));
            }

            if (workerConfig.isTransactionDeduplicationEnabled) {
                List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                removeDuplicates(transactionList, workerConfig.isOrderingWorkerEnabled);
                variables.put(TRANSACTION_LIST, transactionList);
            }
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

    private void removeDuplicates(List<Transaction> transactionList, boolean orderingEnabled) {
        if (orderingEnabled) {
            removeDuplicatesIfOrderingEnabled(transactionList);
            return;
        }
        removeDuplicatesIfOrderingDisabled(transactionList);
    }

    private void removeDuplicatesIfOrderingEnabled(List<Transaction> transactionList) {

        for (int i = 0; i < transactionList.size() - 1; i++) {
            Transaction currentTransaction = transactionList.get(i);
            Transaction nextTransaction = transactionList.get(i + 1);

            if (currentTransaction == null || nextTransaction == null) {
                continue;
            }
            String currentPayeeDetail = fetchPayeeDetail(currentTransaction);
            String nextPayeeDetail = fetchPayeeDetail(nextTransaction);

            if (currentPayeeDetail.equals(nextPayeeDetail)) {
                currentTransaction.setNote("Duplicate transaction.");
            }
        }
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

    private String fetchPayeeDetail(Transaction transaction) {
        String payeeIdentifier = transaction.getPayeeIdentifier();
        String payeeIdentifierType = transaction.getPayeeIdentifierType();
        String amount = transaction.getAmount();
        String currency = transaction.getCurrency();

        return String.format("%s%s%s%s", payeeIdentifier, payeeIdentifierType, amount, currency);
    }

}
