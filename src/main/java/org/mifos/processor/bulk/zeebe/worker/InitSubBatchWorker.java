package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.mifos.processor.bulk.camel.routes.RouteId;
import org.mifos.processor.bulk.schema.SubBatchEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InitSubBatchWorker extends BaseWorker {

    @Autowired
    private ObjectMapper objectMapper;

    // FIXED: Generic-safe, null-safe, no inference issues
    private static List<String> toStringList(Object obj) {
        if (obj == null) {
            return new ArrayList<>();
        }
        if (obj instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    // FIXED: For SUB_BATCH_DETAILS → List<Object> expected
    private static List<Object> toObjectList(Object obj) {
        if (obj == null) {
            return new ArrayList<>();
        }
        if (obj instanceof List<?> list) {
            return new ArrayList<>(list); // safe: List<?> → List<Object>
        }
        return new ArrayList<>();
    }

    @Override
    public void setup() {
        newWorker(Worker.INIT_SUB_BATCH, (client, job) -> {
            logger.info("Started INIT_SUB_BATCH worker");

            Map<String, Object> variables = job.getVariablesAsMap();

            // 100% SAFE LISTS — NO NPE, NO COMPILATION ISSUES
            List<String> subBatches = toStringList(variables.get(SUB_BATCHES));
            List<String> successSubBatches = toStringList(variables.get(INIT_SUCCESS_SUB_BATCHES));
            List<String> failureSubBatches = toStringList(variables.get(INIT_FAILURE_SUB_BATCHES));
            List<Object> subBatchDetails = toObjectList(variables.get(SUB_BATCH_DETAILS));

            // Early exit
            if (subBatches.isEmpty()) {
                logger.info("No sub-batches to process. Completing job early.");
                variables.put(REMAINING_SUB_BATCH, 0);
                variables.put(SUB_BATCHES, new ArrayList<String>());
                variables.put(INIT_SUCCESS_SUB_BATCHES, new ArrayList<String>());
                variables.put(INIT_FAILURE_SUB_BATCHES, new ArrayList<String>());
                client.newCompleteCommand(job.getKey())
                        .variables(variables)
                        .send()
                        .join();
                return;
            }

            // Handle non-splitting mode
            Boolean splittingEnabled = (Boolean) variables.get(SPLITTING_ENABLED);
            if (Boolean.FALSE.equals(splittingEnabled)) {
                String fileName = (String) variables.get(FILE_NAME);
                if (fileName != null && !subBatches.contains(fileName)) {
                    subBatches.add(fileName);
                }
            }

            // Safe remove
            String currentFile = subBatches.remove(0);

            // Parse sub-batch details
            List<SubBatchEntity> subBatchEntityList = objectMapper.convertValue(
                    subBatchDetails,
                    new TypeReference<List<SubBatchEntity>>() {}
            );

            SubBatchEntity subBatchEntity = subBatchEntityList.stream()
                    .filter(e -> e.getRequestFile() != null && e.getRequestFile().contains(currentFile))
                    .findFirst()
                    .orElse(null);

            // Setup Camel exchange
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(TENANT_NAME, variables.get(TENANT_ID));
            exchange.setProperty(SERVER_FILE_NAME, currentFile);
            exchange.setProperty(BATCH_ID, variables.get(BATCH_ID));
            exchange.setProperty(REQUEST_ID, variables.get(REQUEST_ID));
            exchange.setProperty(PURPOSE, variables.get(PURPOSE));
            exchange.setProperty(ZEEBE_VARIABLE, variables);
            exchange.setProperty(SUB_BATCH_ENTITY, subBatchEntity);

            sendToCamelRoute(RouteId.INIT_SUB_BATCH, exchange);

            Boolean failed = exchange.getProperty(INIT_SUB_BATCH_FAILED, Boolean.class);
            if (Boolean.TRUE.equals(failed)) {
                failureSubBatches.add(currentFile);
            } else {
                successSubBatches.add(currentFile);
            }

            // Update Zeebe variables
            variables.put(REMAINING_SUB_BATCH, subBatches.size());
            variables.put(SUB_BATCHES, new ArrayList<>(subBatches));
            variables.put(INIT_SUCCESS_SUB_BATCHES, new ArrayList<>(successSubBatches));
            variables.put(INIT_FAILURE_SUB_BATCHES, new ArrayList<>(failureSubBatches));

            client.newCompleteCommand(job.getKey())
                    .variables(variables)
                    .send()
                    .join();

            logger.info("Completed INIT_SUB_BATCH worker. Remaining sub-batches: {}", subBatches.size());
        });
    }
}