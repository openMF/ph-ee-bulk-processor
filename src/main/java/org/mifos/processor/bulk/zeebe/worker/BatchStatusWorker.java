package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.COMPLETION_RATE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.mifos.processor.bulk.OperationsAppConfig;
import org.mifos.processor.bulk.schema.BatchDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class BatchStatusWorker extends BaseWorker {

    @Autowired
    public OperationsAppConfig operationsAppConfig;

    @Override
    public void setup() {

        newWorker(Worker.BATCH_STATUS, ((client, job) -> {
            logger.info("Started batchStatusWorker");
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());

            Map<String, Object> variables = job.getVariablesAsMap();
            String batchId = (String) variables.get(BATCH_ID);
            String tenantId = (String) variables.get(TENANT_ID);
            BatchDTO batchDTOResponse = invokeBatchAggregationApi(batchId, tenantId);
            float successRate = calculateSuccessPercentage(batchDTOResponse);
            variables.put(COMPLETION_RATE, successRate);
            client.newCompleteCommand(job.getKey()).variables(variables).send();
            logger.info("Completed batchStatusWorker");
        }));
    }

    private float calculateSuccessPercentage(BatchDTO batchDTO) {
        if (batchDTO.getTotal() != null && batchDTO.getTotal() != 0) {
            return (((float) batchDTO.getSuccessful() / batchDTO.getTotal()) * 100);
        }
        return 0;
    }

    private BatchDTO invokeBatchAggregationApi(String batchId, String tenantId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Platform-TenantId", tenantId);
        String url = operationsAppConfig.batchSummaryUrl;

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url).queryParam("batchId", batchId);
        String finalUrl = uriBuilder.toUriString();

        ResponseEntity<String> response = restTemplate.exchange(finalUrl, HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        String batchAggregationResponse = response != null ? response.getBody() : null;
        ObjectMapper objectMapper = new ObjectMapper();
        BatchDTO batchDTO = null;
        try {
            batchDTO = objectMapper.readValue(batchAggregationResponse, BatchDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("Batch summary response: {}", batchDTO);
        return batchDTO;
    }
}
