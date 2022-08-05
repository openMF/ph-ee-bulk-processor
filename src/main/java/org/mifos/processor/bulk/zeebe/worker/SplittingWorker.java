package org.mifos.processor.bulk.zeebe.worker;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class SplittingWorker extends BaseWorker {

    @Override
    public void setup() {

        /**
         * This worker performs below tasks
         * 1. Downloads the original CSV from cloud
         * 2. Splits entire CSV into multiple CSV of sub-batches, based on configured sub-batch size.
         * 3. Uploads the sub-batch CSVs to cloud
         * 4. Sets zeebeVariable [SPLITTING_FAILED, SUB_BATCHES, SUB_BATCH_CREATED]
         */
        newWorker(Worker.SPLITTING, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();
            if (workerConfig.isSplittingWorkerEnabled) {
                variables.put(SPLITTING_FAILED, false);
            }

            String filename = (String) variables.get(FILE_NAME);
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(SERVER_FILE_NAME, filename);

            try {
                sendToCamelRoute(RouteId.SPLITTING, exchange);
                assert !exchange.getProperty(SPLITTING_FAILED, Boolean.class);
            } catch (Exception e) {
               variables.put(SPLITTING_FAILED, true);
            }

            Boolean subBatchCreated = exchange.getProperty(SUB_BATCH_CREATED, Boolean.class);
            List<String> serverSubBatchFileList = exchange.getProperty(SERVER_SUB_BATCH_FILE_NAME_ARRAY, List.class);
            if (!subBatchCreated && serverSubBatchFileList.isEmpty()) {
                // if no sub-batches is created, insert the original filename in sub batch array
                serverSubBatchFileList.add(filename);
            }

            variables.put(SPLITTING_FAILED, false);
            variables.put(SUB_BATCHES, serverSubBatchFileList);
            variables.put(INIT_SUCCESS_SUB_BATCHES, new ArrayList<String>());
            variables.put(INIT_FAILURE_SUB_BATCHES, new ArrayList<String>());
            variables.put(SUB_BATCH_CREATED, subBatchCreated);

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
