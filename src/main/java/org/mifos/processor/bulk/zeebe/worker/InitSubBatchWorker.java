package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.camel.config.CamelProperties.SERVER_FILE_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SUB_BATCH_DETAILS;
import static org.mifos.processor.bulk.camel.config.CamelProperties.SUB_BATCH_ENTITY;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TENANT_NAME;
import static org.mifos.processor.bulk.camel.config.CamelProperties.ZEEBE_VARIABLE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FILE_NAME;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.INIT_FAILURE_SUB_BATCHES;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.INIT_SUB_BATCH_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.INIT_SUCCESS_SUB_BATCHES;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PURPOSE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REMAINING_SUB_BATCH;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.SPLITTING_ENABLED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.SUB_BATCHES;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Override
    public void setup() {

        /**
         * Starts the new worker for initialising sub batches. Performs below tasks 1. Downloads the file from cloud. 2.
         * Parse the data into POJO. 3. Initiates workflow based on the payment_mode
         */
        newWorker(Worker.INIT_SUB_BATCH, (client, job) -> {
            logger.info("Started INIT_SUB_BATCH worker");
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();

            List<String> subBatches = (List<String>) variables.get(SUB_BATCHES);
            if (subBatches == null) {
                subBatches = new ArrayList<>();
            }
            List<String> successSubBatches = (List<String>) variables.get(INIT_SUCCESS_SUB_BATCHES);
            if (successSubBatches == null) {
                successSubBatches = new ArrayList<>();
            }
            List<String> failureSubBatches = (List<String>) variables.get(INIT_FAILURE_SUB_BATCHES);
            if (failureSubBatches == null) {
                failureSubBatches = new ArrayList<>();
            }
            boolean isSplittingEnabled = (boolean) variables.get(SPLITTING_ENABLED);

            if (!isSplittingEnabled) {
                subBatches.add((String) variables.get(FILE_NAME));
            }

            List<Object> subBatchObjectList = (List<Object>) variables.get(SUB_BATCH_DETAILS);
            logger.debug("Subbatch entity list in init sub batch worker: {}", subBatchObjectList);

            List<SubBatchEntity> subBatchEntityList = objectMapper.convertValue(subBatchObjectList, new TypeReference<>() {});

            String fileName = subBatches.remove(0);
            SubBatchEntity subBatchEntity = null;
            if (isSplittingEnabled) {
                for (SubBatchEntity subBatch : subBatchEntityList) {
                    if (subBatch.getRequestFile().contains(fileName)) {
                        subBatchEntity = subBatch;
                        logger.info("SubBatchEntity found");
                        break;
                    }
                }
                logger.debug("BatchEntity for this subbatch is {}", objectMapper.writeValueAsString(subBatchEntity));
            }
            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(TENANT_NAME, variables.get(TENANT_ID));
            exchange.setProperty(SERVER_FILE_NAME, fileName);
            exchange.setProperty(BATCH_ID, variables.get(BATCH_ID));
            exchange.setProperty(REQUEST_ID, variables.get(REQUEST_ID));
            exchange.setProperty(PURPOSE, variables.get(PURPOSE));
            exchange.setProperty(ZEEBE_VARIABLE, variables);
            exchange.setProperty(SUB_BATCH_ENTITY, subBatchEntity);

            sendToCamelRoute(RouteId.INIT_SUB_BATCH, exchange);

            Boolean subBatchFailed = exchange.getProperty(INIT_SUB_BATCH_FAILED, Boolean.class);
            if (subBatchFailed != null && subBatchFailed) {
                failureSubBatches.add(fileName);
            } else {
                successSubBatches.add(fileName);
            }

            variables.put(REMAINING_SUB_BATCH, subBatches.size());
            variables.put(SUB_BATCHES, subBatches);
            variables.put(INIT_SUCCESS_SUB_BATCHES, successSubBatches);
            variables.put(INIT_FAILURE_SUB_BATCHES, failureSubBatches);

            client.newCompleteCommand(job.getKey()).variables(variables).send();
            logger.info("Completed INIT_SUB_BATCH worker. Remaining subbatches {}", subBatches.size());
        });
    }

}
