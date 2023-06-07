package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.LOCAL_FILE_PATH;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_SUB_BATCH_FILE_NAME_ARRAY;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SUB_BATCH_COUNT;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SUB_BATCH_CREATED;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SUB_BATCH_FILE_ARRAY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.SPLITTING_FAILED;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SplittingRoute extends BaseRouteBuilder {

    @Value("${config.splitting.sub-batch-size}")
    private int subBatchSize;

    @Override
    public void configure() throws Exception {

        /**
         * Base route for starting the splitting process. Refer below routes for more info 1.
         * direct:create-sub-batch-file 2. direct:upload-sub-batch-file
         */
        from(RouteId.SPLITTING.getValue()).id(RouteId.SPLITTING.getValue()).log("Starting route " + RouteId.SPLITTING.name())
                .to("direct:download-file").to("direct:create-sub-batch-file").choice()
                .when(exchange -> exchange.getProperty(SUB_BATCH_CREATED, Boolean.class)).to("direct:upload-sub-batch-file").otherwise()
                .log("No sub batch created, so skipping upload").end().process(exchange -> exchange.setProperty(SPLITTING_FAILED, false));

        // Creates the sub-batch CSVs
        from("direct:create-sub-batch-file").id("direct:create-sub-batch-file").log("Creating sub-batch file").process(exchange -> {
            String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
            BufferedReader reader = new BufferedReader(new FileReader(filepath));
            String header = reader.readLine() + System.lineSeparator();
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

            List<String> subBatchFile = new ArrayList<>();
            int subBatchCount = 1;
            for (int i = 0; i < lines.size(); i += subBatchSize) {
                String filename = System.currentTimeMillis() + "_" + "sub-batch-" + subBatchCount + ".csv";
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
            exchange.setProperty(SUB_BATCH_FILE_ARRAY, subBatchFile);
            exchange.setProperty(SUB_BATCH_COUNT, subBatchFile.size());
            exchange.setProperty(SUB_BATCH_CREATED, true);
            exchange.setProperty(SERVER_SUB_BATCH_FILE_NAME_ARRAY, new ArrayList<String>());
        });

        // Iterate through each CSVs of sub-batches and uploads in cloud
        from("direct:upload-sub-batch-file").id("direct:upload-sub-batch-file").log("Starting upload of sub-batch file")
                .loopDoWhile(exchange -> exchange.getProperty(SUB_BATCH_FILE_ARRAY, List.class).size() > 0).process(exchange -> {
                    List<String> subBatchFile = exchange.getProperty(SUB_BATCH_FILE_ARRAY, List.class);
                    exchange.setProperty(LOCAL_FILE_PATH, subBatchFile.remove(0));
                    exchange.setProperty(SUB_BATCH_FILE_ARRAY, subBatchFile);
                }).to("direct:upload-file").process(exchange -> {
                    String serverFilename = exchange.getProperty(SERVER_FILE_NAME, String.class);
                    List<String> serverSubBatchFile = exchange.getProperty(SERVER_SUB_BATCH_FILE_NAME_ARRAY, List.class);
                    serverSubBatchFile.add(serverFilename);
                    exchange.setProperty(SERVER_SUB_BATCH_FILE_NAME_ARRAY, serverSubBatchFile);
                });
    }
}
