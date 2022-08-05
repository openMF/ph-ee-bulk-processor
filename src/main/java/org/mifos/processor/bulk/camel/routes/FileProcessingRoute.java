package org.mifos.processor.bulk.camel.routes;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.mifos.processor.bulk.camel.config.CamelProperties.*;

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
                .process(exchange -> {
                    String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    List<org.mifos.processor.bulk.schema.CsvSchema> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);

                    // getting header
                    Boolean overrideHeader = exchange.getProperty(OVERRIDE_HEADER, Boolean.class);
                    String header;
                    if (overrideHeader == null || !overrideHeader) {
                        BufferedReader reader = new BufferedReader(new FileReader(filepath));
                        header = reader.readLine() + System.lineSeparator();
                        reader.close();
                    } else {
                        header = transactionList.get(0).getCsvHeader() + System.lineSeparator();
                    }

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
                    logger.info("Update complete");
                });
    }
}
