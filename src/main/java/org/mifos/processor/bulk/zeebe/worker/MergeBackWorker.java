package org.mifos.processor.bulk.zeebe.worker;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@Component
public class MergeBackWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.MERGE_BACK, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();
            if (workerConfig.isMergeBackWorkerEnabled) {
                variables.put(MERGE_FAILED, false);
            }

            int mergeIteration = (int) variables.getOrDefault(MERGE_ITERATION, 1);
            List<String> subBatches = (List<String>) variables.get(SUB_BATCHES);
            List<String> successSubBatches = (List<String>) variables.get(INIT_SUCCESS_SUB_BATCHES);
            List<String> failureSubBatches = (List<String>) variables.get(INIT_FAILURE_SUB_BATCHES);

            for (int i = 0; i < successSubBatches.size(); i++) {
                String initFile = successSubBatches.remove(i);
                successSubBatches.add(i, String.format("Result_%s", initFile));
            }

            List<String> mergeFileList = (List<String>) variables.get(MERGE_FILE_LIST);
            if (mergeFileList == null) {
                mergeFileList = new ArrayList<>();
                mergeFileList.addAll(successSubBatches);
                mergeFileList.addAll(failureSubBatches);
                mergeFileList.addAll(subBatches);
            }

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(MERGE_FILE_LIST, mergeFileList);
            exchange.setProperty(MERGE_ITERATION, mergeIteration);
            exchange.setProperty(BATCH_ID, variables.get(BATCH_ID));

            logger.info("Merge list: {}", mergeFileList);

            sendToCamelRoute(RouteId.MERGE_BACK, exchange);

            boolean mergeCompleted = exchange.getProperty(MERGE_COMPLETED, Boolean.class);
            if (mergeCompleted) {
                variables.put(MERGE_FAILED, exchange.getProperty(MERGE_FAILED, Boolean.class));
                String resultFile = exchange.getProperty(RESULT_FILE, String.class);
                if (resultFile != null && !resultFile.isEmpty()) {
                    variables.put(RESULT_FILE, resultFile);
                }
            }

            variables.put(MERGE_FILE_LIST, exchange.getProperty(MERGE_FILE_LIST, List.class));
            variables.put(MERGE_COMPLETED, mergeCompleted);
            variables.put(MERGE_ITERATION, ++mergeIteration);


            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
