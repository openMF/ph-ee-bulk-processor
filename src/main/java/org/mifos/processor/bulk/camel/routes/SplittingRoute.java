package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.LOCAL_FILE_PATH;
import static org.mifos.processor.bulk.camel.config.CamelProperties.REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_SUB_BATCH_FILE_NAME_ARRAY;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SUB_BATCH_COUNT;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SUB_BATCH_CREATED;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SUB_BATCH_DETAILS;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SUB_BATCH_FILE_ARRAY;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.camel.config.CamelProperties.ZEEBE_VARIABLE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYER_IDENTIFIER;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.SPLITTING_FAILED;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.camel.LoggingLevel;
import org.mifos.processor.bulk.schema.SubBatchEntity;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SplittingRoute extends BaseRouteBuilder {

    @Value("${config.splitting.sub-batch-size}")
    private int subBatchSize;
    @Autowired
    private CsvMapper csvMapper;
    @Value("${config.partylookup.enable}")
    private boolean partyLookupEnabled;

    @Override
    public void configure() throws Exception {

        /**
         * Base route for starting the splitting process. Refer below routes for more info 1.
         * direct:create-sub-batch-file 2. direct:upload-sub-batch-file
         */
        from(RouteId.SPLITTING.getValue()).id(RouteId.SPLITTING.getValue()).log("Starting route " + RouteId.SPLITTING.name())
                .to("direct:download-file").to("direct:get-transaction-array").to("direct:create-sub-batch-file").choice()
                .when(exchange -> exchange.getProperty(SUB_BATCH_CREATED, Boolean.class)).to("direct:upload-sub-batch-file").otherwise()
                .log("No sub batch created, so skipping upload").end().process(exchange -> exchange.setProperty(SPLITTING_FAILED, false));

        // Creates the sub-batch CSVs
        from("direct:create-sub-batch-file").id("direct:create-sub-batch-file").log("Creating sub-batch file").process(exchange -> {
            String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
            BufferedReader reader = new BufferedReader(new FileReader(filepath));
            String header = reader.readLine() + System.lineSeparator();
            List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
            List<String> subBatchFile = new ArrayList<>();
            Set<String> distinctPayeeIds = transactionList.stream().map(Transaction::getPayeeDfspId).collect(Collectors.toSet());
            logger.info("Payee id {}", distinctPayeeIds);
            Boolean batchAccountLookup = (Boolean) exchange.getProperty("batchAccountLookup");
            if (partyLookupEnabled && batchAccountLookup) {
                // Create a map to store transactions for each payeeid
                Map<String, List<Transaction>> transactionsByPayeeId = new HashMap<>();

                // Split the list based on distinct payeeids
                for (String payeeId : distinctPayeeIds) {
                    List<Transaction> transactionsForPayee = transactionList.stream()
                            .filter(transaction -> payeeId.equals(transaction.getPayeeDfspId())).collect(Collectors.toList());

                    transactionsByPayeeId.put(payeeId, transactionsForPayee);
                }

                for (String payeeId : distinctPayeeIds) {
                    List<Transaction> transactionsForSpecificPayee = transactionsByPayeeId.get(payeeId);
                    String filename = UUID.randomUUID() + "_" + "sub-batch-" + payeeId + ".csv";
                    logger.info("Created sub-batch with file name {}", filename);
                    CsvSchema csvSchema = csvMapper.schemaFor(Transaction.class);
                    csvSchema = csvSchema.withHeader();
                    File file = new File(filename);
                    SequenceWriter writer = csvMapper.writerWithSchemaFor(Transaction.class).with(csvSchema).writeValues(file);
                    for (Transaction transaction : transactionsForSpecificPayee) {
                        writer.write(transaction);
                    }
                    subBatchFile.add(filename);
                }
            } else {
                List<String> lines = new ArrayList<>();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                reader.close();

                if (lines.size() <= subBatchSize) {
                    exchange.setProperty(SUB_BATCH_CREATED, false);
                    exchange.setProperty(SERVER_SUB_BATCH_FILE_NAME_ARRAY, new ArrayList<String>());
                    logger.info("Skipping creating sub batch, as batch size is less than configured sub-batch size");
                    return;
                }

                int subBatchCount = 1;
                for (int i = 0; i < lines.size(); i += subBatchSize) {
                    String filename = UUID.randomUUID() + "_" + "sub-batch-" + subBatchCount + ".csv";
                    FileWriter writer = new FileWriter(filename);
                    writer.write(header);
                    for (int j = i; j < Math.min(i + subBatchSize, lines.size()); j++) {
                        writer.write(lines.get(j) + System.lineSeparator());
                    }
                    writer.close();
                    logger.info("Created sub-batch with file name {}", filename);
                    subBatchFile.add(filename);
                    subBatchCount++;
                }
            }
            exchange.setProperty(SUB_BATCH_FILE_ARRAY, subBatchFile);
            exchange.setProperty(SUB_BATCH_COUNT, subBatchFile.size());
            exchange.setProperty(SUB_BATCH_CREATED, true);
            exchange.setProperty(SERVER_SUB_BATCH_FILE_NAME_ARRAY, new ArrayList<String>());
        });

        // Iterate through each CSVs of sub-batches and uploads in cloud
        from("direct:upload-sub-batch-file").id("direct:upload-sub-batch-file").log("Starting upload of sub-batch file")
                .loopDoWhile(exchange -> exchange.getProperty(SUB_BATCH_FILE_ARRAY, List.class).size() > 0).process(exchange -> {
                    List<String> subBatchFile = exchange.getProperty(SUB_BATCH_FILE_ARRAY, List.class);
                    String localFilePath = subBatchFile.remove(0);
                    exchange.setProperty(LOCAL_FILE_PATH, localFilePath);
                    exchange.setProperty(SUB_BATCH_FILE_ARRAY, subBatchFile);
                    logger.debug("Local file path: {}", localFilePath);
                    logger.debug("Sub batch file array: {}, ", subBatchFile);
                }).log(LoggingLevel.DEBUG, "LOCAL_FILE_PATH: ${exchangeProperty." + LOCAL_FILE_PATH + "}")
                .to("direct:generate-sub-batch-entity").log("direct:generate-sub-batch-entity completed").to("direct:upload-file")
                .process(exchange -> {
                    String serverFilename = exchange.getProperty(SERVER_FILE_NAME, String.class);
                    List<String> serverSubBatchFile = exchange.getProperty(SERVER_SUB_BATCH_FILE_NAME_ARRAY, List.class);
                    serverSubBatchFile.add(serverFilename);
                    exchange.setProperty(SERVER_SUB_BATCH_FILE_NAME_ARRAY, serverSubBatchFile);
                    logger.debug("Server subbatch filename array: {}", serverSubBatchFile);
                });

        // generate subBatchEntityDetails, make sure [LOCAL_FILE_PATH] has the absolute sub batch file path
        from("direct:generate-sub-batch-entity").id("direct:generate-sub-batch-entity").log("Generating sub batch entity")
                .to("direct:get-transaction-array").process(exchange -> {
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    Map<String, Object> zeebeVariables = exchange.getProperty(ZEEBE_VARIABLE, Map.class);
                    String serverFileName = exchange.getProperty(LOCAL_FILE_PATH, String.class);

                    logger.info("Generating sub batch entity for file {}", serverFileName);
                    if (transactionList.isEmpty()) {
                        logger.info("Transaction list is empty");
                        return;
                    }

                    Long totalAmount = getTotalAmount(transactionList);

                    SubBatchEntity subBatchEntity = getDefaultSubBatchEntity();
                    subBatchEntity.setBatchId((String) zeebeVariables.get(BATCH_ID));
                    subBatchEntity.setSubBatchId(UUID.randomUUID().toString());
                    subBatchEntity.setRequestId((String) zeebeVariables.get(REQUEST_ID));
                    subBatchEntity.setCorrelationId((String) zeebeVariables.get(CLIENT_CORRELATION_ID));
                    subBatchEntity.setPayerFsp((String) zeebeVariables.get(PAYER_IDENTIFIER));
                    subBatchEntity.setRegisteringInstitutionId((String) zeebeVariables.get(REGISTERING_INSTITUTE_ID));
                    subBatchEntity.setPaymentMode(transactionList.get(0).getPaymentMode());
                    subBatchEntity.setRequestFile(serverFileName);
                    subBatchEntity.setTotalTransactions((long) transactionList.size());
                    subBatchEntity.setOngoing((long) transactionList.size());
                    subBatchEntity.setTotalAmount(totalAmount);
                    subBatchEntity.setOngoingAmount(totalAmount);
                    subBatchEntity.setStartedAt(new Date(System.currentTimeMillis()));

                    logger.debug("SubBatchEntity: {}", objectMapper.writeValueAsString(subBatchEntity));
                    // update the sub batch details array
                    List<SubBatchEntity> subBatchEntityList = exchange.getProperty(SUB_BATCH_DETAILS, List.class);
                    subBatchEntityList.add(subBatchEntity);
                    exchange.setProperty(SUB_BATCH_DETAILS, subBatchEntityList);
                    logger.debug("generate-sub-batch-entity route end: {}", objectMapper.writeValueAsString(subBatchEntityList));
                });
    }

    private SubBatchEntity getDefaultSubBatchEntity() {
        SubBatchEntity subBatchEntity = new SubBatchEntity();
        subBatchEntity.setAllEmptyAmount();
        return subBatchEntity;
    }

    private long getTotalAmount(List<Transaction> transactionList) {
        long totalAmount = 0L;
        for (Transaction transaction : transactionList) {
            totalAmount += Long.parseLong(transaction.getAmount());
        }
        return totalAmount;
    }
}
