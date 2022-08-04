package org.mifos.processor.bulk.camel.routes;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ORDERED_BY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ORDERING_FAILED;

@Component
public class OrderingRoute extends BaseRouteBuilder {

    @Autowired
    private CsvMapper csvMapper;

    @Value("${config.ordering.field}")
    private String orderingField;


    @Override
    public void configure() {

        /**
         * Base route for kicking off ordering logic. Performs below tasks.
         * 1. Downloads the csv form cloud.
         * 2. Builds the [Transaction] array using CsvMapper.
         * 3. Re-order the array generated in step1 based on [orderingField].
         * 4. Update file with the updated data.
         * 5. Uploads the updated file in cloud.
         */
        from(RouteId.ORDERING.getValue())
                .id(RouteId.ORDERING.getValue())
                .log("Starting route " + RouteId.ORDERING.name())
                .to("direct:download-file")
                .to("direct:get-transaction-array")
                .to("direct:order-data")
                .to("direct:update-file")
                .to("direct:upload-file")
                .process(exchange -> {
                    exchange.setProperty(ORDERING_FAILED, false);
                    exchange.setProperty(ORDERED_BY, orderingField);
                });

        // Parse the [Transaction] array from the csv file
        from("direct:get-transaction-array")
                .id("direct:get-transaction-array")
                .log("Starting route direct:get-transaction-array")
                .process(exchange -> {
                    String filename = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    CsvSchema schema = CsvSchema.emptySchema().withHeader();
                    FileReader reader = new FileReader(filename);
                    MappingIterator<Transaction> readValues = csvMapper.readerWithSchemaFor(Transaction.class).with(schema).readValues(reader);
                    List<Transaction> transactionList = new ArrayList<>();
                    while (readValues.hasNext()) {
                        Transaction current = readValues.next();
                        transactionList.add(current);
                    }
                    reader.close();
                    exchange.setProperty(TRANSACTION_LIST, transactionList);
                });

        // re-order the array of [Transaction] based on [orderingField]
        from("direct:order-data")
                .id("direct:order-data")
                .log("Starting route direct:order-data")
                .process(exchange -> {
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    HashMap<String, List<Transaction>> stringListHashMap = new HashMap<>();
                    transactionList.forEach((transaction) -> {
                        String key;
                        switch (orderingField) {
                            case "id":
                                key = "" + transaction.getId();
                                break;
                            case "request_id":
                                key = transaction.getRequest_id();
                                break;
                            case "account_number":
                                key = transaction.getAccount_number();
                                break;
                            case "amount":
                                key = transaction.getAmount();
                                break;
                            case "currency":
                                key = transaction.getCurrency();
                                break;
                            case "note":
                                key = transaction.getNote();
                                break;
                            default:
                                key = transaction.getPayment_mode();
                                break;
                        }

                        if (stringListHashMap.containsKey(key)) {
                            stringListHashMap.get(key).add(transaction);
                        } else {
                            stringListHashMap.put(key, new ArrayList<Transaction>() {{
                                add(transaction);
                            }});
                        }
                    });
                    transactionList.clear();
                    stringListHashMap.forEach((s, transactions) -> transactionList.addAll(transactions));
                    exchange.setProperty(TRANSACTION_LIST, transactionList);
                });

        // updates the data in local file
        from("direct:update-file")
                .id("direct:update-file")
                .log("Starting route direct:update-file")
                .process(exchange -> {
                    String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);

                    // getting header
                    BufferedReader reader = new BufferedReader(new FileReader(filepath));
                    String header = reader.readLine() + System.lineSeparator();
                    reader.close();

                    FileWriter writer = new FileWriter(filepath);
                    writer.write(header);

                    // updating file with re-ordered data
                    transactionList.forEach(transaction -> {
                        try {
                            writer.write(transaction.getCsvString() + System.lineSeparator());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    writer.close();
                });
    }
}
