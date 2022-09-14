package org.mifos.processor.bulk.camel.routes;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class FileProcessingRoute extends BaseRouteBuilder {

    @Autowired
    private CsvMapper csvMapper;

    @Override
    public void configure() {

        /**
         * Parse the [Transaction] array from the csv file
         * exchangeInput: [LOCAL_FILE_PATH] the absolute path to the csv file
         * exchangeOutput: [TRANSACTION_LIST] containing the list of [Transaction]
         */
        from("direct:get-transaction-array")
                .id("direct:get-transaction-array")
                .log("Starting route direct:get-transaction-array")
                .process(exchange -> {
                    Long totalAmount = 0L;
                    Long failedAmount = 0L;
                    Long completedAmount = 0L;
                    String filename = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    CsvSchema schema = CsvSchema.emptySchema().withHeader();
                    FileReader reader = new FileReader(filename);
                    MappingIterator<Transaction> readValues = csvMapper.readerWithSchemaFor(Transaction.class).with(schema).readValues(reader);
                    List<Transaction> transactionList = new ArrayList<>();
                    while (readValues.hasNext()) {
                        Transaction current = readValues.next();
                        transactionList.add(current);
                        totalAmount += Long.parseLong(current.getAmount());
                    }
                    reader.close();
                    exchange.setProperty(TRANSACTION_LIST, transactionList);
                    exchange.setProperty(TOTAL_AMOUNT, totalAmount);
                    exchange.setProperty(ONGOING_AMOUNT, totalAmount); // initially ongoing amount is same as total amount
                    exchange.setProperty(FAILED_AMOUNT, failedAmount);
                    exchange.setProperty(COMPLETED_AMOUNT, completedAmount);
                });

        /**
         * updates the data in local file
         * exchangeInput:
         *      [LOCAL_FILE_PATH] the absolute path to the csv file
         *      [TRANSACTION_LIST] containing the list of [Transaction]
         *      [OVERRIDE_HEADER] if set to true will override the header or else use the existing once in csv file
         */
        from("direct:update-file")
                .id("direct:update-file")
                .log("Starting route direct:update-file")
                .to("direct:update-file-v2")
                .log("Update complete");

        /**
         * this is backward compatible version of update-file route for new CSV schema
         * exchangeInput:
         *      [LOCAL_FILE_PATH] the absolute path to the csv file
         *      [TRANSACTION_LIST] containing the list of [Transaction]
         *      [OVERRIDE_HEADER] if set to true will override the header or else use the existing once in csv file
         */
        from("direct:update-file-v2")
                .id("direct:update-file-v2")
                .log("Starting route direct:update-file-v2")
                .process(exchange -> {
                    String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);

                    // getting header
                    Boolean overrideHeader = exchange.getProperty(OVERRIDE_HEADER, Boolean.class);
                    CsvSchema csvSchema = csvMapper.schemaFor(Transaction.class);
                    if (overrideHeader) {
                        csvSchema = csvSchema.withHeader();
                    } else {
                        csvSchema = csvSchema.withoutHeader();
                    }

                    File file = new File(filepath);
                    SequenceWriter writer = csvMapper.writerWithSchemaFor(Transaction.class).with(csvSchema).writeValues(file);
                    for (Transaction transaction: transactionList) {
                        writer.write(transaction);
                    }
                });
    }
}
