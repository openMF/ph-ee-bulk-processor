package org.mifos.processor.bulk.camel.routes;

import org.mifos.processor.bulk.utility.Utils;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.List;
import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class MergeBackRoute extends BaseRouteBuilder {


    @Override
    public void configure() throws Exception {

        /**
         * Base route for kicking off merge back logic. Performs below tasks.
         * 1. Picks the top two files from the array of files to be merged.
         * 2. Merges them into single CSV.
         * 3. Uploads the CSV to S3.
         * 4. Updated the exchange variables.
         */
        from(RouteId.MERGE_BACK.getValue())
                .id(RouteId.MERGE_BACK.getValue())
                .log("Starting route " + RouteId.MERGE_BACK.name())
                .choice()
                .when(exchange -> exchange.getProperty(MERGE_FILE_LIST, List.class).size() == 0)
                .log("Nothing to merge")
                .setProperty(MERGE_FAILED, constant(false))
                .setProperty(MERGE_COMPLETED, constant(true))
                .when(exchange -> exchange.getProperty(MERGE_FILE_LIST, List.class).size() == 1)
                .process(exchange -> {
                    exchange.setProperty(MERGE_FAILED, false);
                    exchange.setProperty(MERGE_COMPLETED, true);
                    exchange.setProperty(RESULT_FILE, exchange.getProperty(MERGE_FILE_LIST, List.class).get(0));
                })
                .otherwise()
                .to("direct:start-merge")
                .endChoice();

        // starts the merge process, merges the file and uploads the file in s3
        from("direct:start-merge")
                .id("direct:start-merge")
                .log("Starting route direct:start-merge")
                .to("direct:download-file-to-be-merged")
                .process(exchange -> {
                    String file1 = exchange.getProperty(FILE_1, String.class);
                    String file2 = exchange.getProperty(FILE_2, String.class);

                    String mergedFile = Utils.mergeCsvFile(file1, file2);
                    if (mergedFile == null) {
                        exchange.setProperty(MERGE_COMPLETED, false);
                        return;
                    }
                    if (exchange.getProperty(MERGE_ITERATION, Integer.class) == 1) {
                        // generate new name for merged file in case of first iteration
                        String newFileName = System.currentTimeMillis() + "_" +
                                exchange.getProperty(BATCH_ID, String.class) + ".csv";
                        new File(mergedFile).renameTo(new File(newFileName));
                        exchange.setProperty(LOCAL_FILE_PATH, newFileName);
                    } else {
                        exchange.setProperty(LOCAL_FILE_PATH, mergedFile);
                    }
                })
                .to("direct:upload-file")
                .process(exchange -> {
                    String mergedFileServerName = exchange.getProperty(SERVER_FILE_NAME, String.class);
                    List<String> mergeList = exchange.getProperty(MERGE_FILE_LIST, List.class);
                    String first = mergeList.remove(0);
                    String second = mergeList.remove(0);
                    logger.info("Merge iteration {}, for list, {}", exchange.getProperty(MERGE_ITERATION), mergeList);
                    log.info("Merged files {} and {}", first, second);
                    mergeList.add(0, mergedFileServerName);


                    if (mergeList.size() == 1) {
                        exchange.setProperty(MERGE_FAILED, false);
                        exchange.setProperty(MERGE_COMPLETED, true);
                        exchange.setProperty(RESULT_FILE, Utils.getAwsFileUrl(awsS3BaseUrl,mergedFileServerName));
                    } else {
                        exchange.setProperty(MERGE_COMPLETED, false);
                    }

                    exchange.setProperty(MERGE_FILE_LIST, mergeList);

                    // make sures to remove the files from local storage
                    new File(exchange.getProperty(FILE_1, String.class)).delete();
                    new File(exchange.getProperty(FILE_2, String.class)).delete();
                });

        // downloads the two files(using FIFO access pattern) from s3 which is to be merged.
        from("direct:download-file-to-be-merged")
                .id("direct:download-file-to-be-merged")
                .log("Starting route direct:download-file-to-be-merged")
                .log("Downloading files to be merged")
                .process(exchange -> {
                    List<String> mergeList = exchange.getProperty(MERGE_FILE_LIST, List.class);
                    exchange.setProperty(SERVER_FILE_NAME, mergeList.get(0));
                })
                .to("direct:download-file") // downloading first file
                .setProperty(FILE_1, exchangeProperty(LOCAL_FILE_PATH))
                .process(exchange -> {
                    List<String> mergeList = exchange.getProperty(MERGE_FILE_LIST, List.class);
                    exchange.setProperty(SERVER_FILE_NAME, mergeList.get(1));
                })
                .to("direct:download-file") // downloading second file
                .setProperty(FILE_2, exchangeProperty(LOCAL_FILE_PATH));
    }
}
