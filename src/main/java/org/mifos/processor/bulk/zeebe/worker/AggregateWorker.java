package org.mifos.processor.bulk.zeebe.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mifos.processor.bulk.schema.BatchDTO;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;

public class AggregateWorker extends BaseWorker{

    @Override
    public void setup() {

        newWorker(Worker.AGGREGATE, ((client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());

            // fetch all the variables that i need
            Map<String, Object> variables = job.getVariablesAsMap();
            String batchId = (String) variables.get(BATCH_ID);
            String tenantId = (String) variables.get(TENANT_ID);



            // make the api call to batch aggregate api using rest template
            invokeBatchAggregationApi(batchId);

            // set the variables for new worker


        }));

    }

    private void invokeBatchAggregationApi(String batchId) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:5002/batch/{batchId}";
        url = url.replace("{batchId}", batchId);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);

        String batchAggregationResponse = response != null ? response.getBody() : null;
        ObjectMapper objectMapper = new ObjectMapper();
        BatchDTO batchDTO = null;
        try {
            batchDTO = objectMapper.readValue(batchAggregationResponse, BatchDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("Batch aggregation response: {}", batchDTO);
    }
}
