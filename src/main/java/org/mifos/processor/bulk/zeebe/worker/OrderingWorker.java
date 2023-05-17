package org.mifos.processor.bulk.zeebe.worker;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class OrderingWorker extends BaseWorker {

    @Override
    public void setup() {

        /**
         * This worker is responsible for ordering the data set based on field configuration. Performs below tasks.
         * 1. Downloads the file from cloud.
         * 2. Parse the data into POJO.
         * 3. Re-order the data based on field configured in application.yaml
         * 4. Uploads the updated file in cloud
         */
        newWorker(Worker.ORDERING, (client, job) -> {
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
            List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
            variables.put(TRANSACTION_LIST, removeDuplicates(transactionList, workerConfig.isOrderingWorkerEnabled));
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

    private List<Transaction> removeDuplicates(List<Transaction> transactionList, boolean orderingEnabled){
        if(orderingEnabled){
            return removeDuplicatesIfOrderingEnabled(transactionList);
        }
        return removeDuplicatesIfOrderingDisabled(transactionList);
    }

    private List<Transaction> removeDuplicatesIfOrderingEnabled(List<Transaction> transactionList){
        List<Transaction> uniqueTransactions = new ArrayList<>();

        for (int i = 0; i <transactionList.size()-1; i++) {
            Transaction currentTransaction = transactionList.get(i);
            Transaction previousTransaction = transactionList.get(i + 1);

            String currentPayeeDetail = fetchPayeeDetail(currentTransaction);
            String previousPayeeDetail = fetchPayeeDetail(previousTransaction);
            if (!currentPayeeDetail.equals(previousPayeeDetail)) {
                uniqueTransactions.add(currentTransaction);
            }
        }
        uniqueTransactions.add(transactionList.get(transactionList.size()-1));
        return uniqueTransactions;
    }

    private List<Transaction> removeDuplicatesIfOrderingDisabled(List<Transaction> transactionList){
        Set<String> set = new HashSet<>();
        List<Transaction> uniqueTransactions = new ArrayList<>();

        for(Transaction transaction : transactionList){
            String payeeDetail = fetchPayeeDetail(transaction);
            if(!set.contains(payeeDetail)){
                uniqueTransactions.add(transaction);
                set.add(payeeDetail);
            }
        }
        return uniqueTransactions;
    }

    private String fetchPayeeDetail(Transaction transaction){
        String payeeIdentifier = transaction.getPayeeIdentifier();
        String payeeIdentifierType = transaction.getPayeeIdentifierType();
        String amount = transaction.getAmount();
        String currency = transaction.getCurrency();
        return payeeIdentifierType + payeeIdentifier + amount + currency;
    }

}
