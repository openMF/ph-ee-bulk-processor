package org.mifos.processor.bulk.zeebe.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mifos.processor.bulk.schema.BatchDTO;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

public class AggregateWorker extends BaseWorker{

    @Override
    public void setup() {

        newWorker(Worker.BATCH_AGGREGATE, ((client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());

            Map<String, Object> variables = job.getVariablesAsMap();
            String batchId = (String) variables.get(BATCH_ID);
            String tenantId = (String) variables.get(TENANT_ID);

            BatchDTO batchDTOResponse = invokeBatchAggregationApi(batchId, tenantId);
            long successRate = calculateSuccessPercentage(batchDTOResponse);
            variables.put(COMPLETION_RATE, successRate);

            client.newCompleteCommand(job.getKey()).variables(variables).send();

        }));
    }

    private long calculateSuccessPercentage(BatchDTO batchDTO){
        if(batchDTO.getTotal()!=null && batchDTO.getTotal()!=0){
            return (long) (((double) batchDTO.getSuccessful() / batchDTO.getTotal()) * 100);
        }
        return 0L;
    }

    private BatchDTO invokeBatchAggregationApi(String batchId, String tenantId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Platform-TenantId", tenantId);
        String url = "http://localhost:5002/batch/{batchId}";
        url = url.replace("{batchId}", batchId);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(null, headers), String.class);
        String batchAggregationResponse = response != null ? response.getBody() : null;
        ObjectMapper objectMapper = new ObjectMapper();
        BatchDTO batchDTO = null;
        try {
            batchDTO = objectMapper.readValue(batchAggregationResponse, BatchDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("Batch aggregation response: {}", batchDTO);
        return batchDTO;
    }
}
