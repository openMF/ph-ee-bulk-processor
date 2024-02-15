package org.mifos.processor.bulk.service;

import static org.mifos.processor.bulk.camel.config.CamelProperties.LOCAL_FILE_PATH;
import static org.mifos.processor.bulk.camel.config.CamelProperties.OVERRIDE_HEADER;
import static org.mifos.processor.bulk.camel.config.CamelProperties.RESULT_TRANSACTION_LIST;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST_LENGTH;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETED_AMOUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FAILED_AMOUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ONGOING_AMOUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TOTAL_AMOUNT;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.schema.TransactionResult;
import org.mifos.processor.bulk.utility.CsvWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FileProcessingRouteService {

    @Autowired
    private CsvMapper csvMapper;

    public void getTxnArray(Exchange exchange) throws IOException {
        Double totalAmount = 0.0;
        Long failedAmount = 0L;
        Long completedAmount = 0L;
        String filename = exchange.getProperty(LOCAL_FILE_PATH, String.class);
        log.debug("Local file path: {}", filename);
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        log.info("Filename: {}", filename);
        FileReader reader = new FileReader(filename);
        MappingIterator<Transaction> readValues = csvMapper.readerWithSchemaFor(Transaction.class).with(schema).readValues(reader);
        List<Transaction> transactionList = new ArrayList<>();
        while (readValues.hasNext()) {
            Transaction current = readValues.next();
            transactionList.add(current);
            totalAmount += Double.parseDouble(current.getAmount());
        }
        reader.close();
        exchange.setProperty(TRANSACTION_LIST, transactionList);
        exchange.setProperty(TRANSACTION_LIST_LENGTH, transactionList.size());
        exchange.setProperty(TOTAL_AMOUNT, totalAmount);
        exchange.setProperty(ONGOING_AMOUNT, totalAmount);
        exchange.setProperty(FAILED_AMOUNT, failedAmount);
        exchange.setProperty(COMPLETED_AMOUNT, completedAmount);
    }

    public void updateResultFile(Exchange exchange) throws IOException {
        String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
        List<TransactionResult> transactionList = exchange.getProperty(RESULT_TRANSACTION_LIST, List.class);

        Boolean overrideHeader = exchange.getProperty(OVERRIDE_HEADER, Boolean.class);

        CsvWriter.writeToCsv(transactionList, TransactionResult.class, csvMapper, overrideHeader, filepath);
    }

    public void updateFile(Exchange exchange) throws IOException {
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
        for (Transaction transaction : transactionList) {
            writer.write(transaction);
        }
    }
}
