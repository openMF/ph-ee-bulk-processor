package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

import java.util.Map;
import org.mifos.processor.bulk.schema.AuthorizationRequest;
import org.mifos.processor.bulk.schema.AuthorizationResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthorizationWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.AUTHORIZATION, (client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();

            if (!workerConfig.isAuthorizationWorkerEnabled) {
                variables.put(AUTHORIZATION_SUCCESSFUL, true);
                client.newCompleteCommand(job.getKey()).variables(variables).send();
                return;
            }

            String payerIdentifier = (String) variables.get("payerIdentifier");
            String totalBatchAmount = (String) variables.get("partyLookupSuccessfulTransactionAmount");
            String currency = (String) variables.get("currency");

            String batchId = (String) variables.get(BATCH_ID);
            String fileName = (String) variables.get(FILE_NAME);
            String clientCorrelationId = Long.toString(job.getKey());

            AuthorizationRequest requestPayload = new AuthorizationRequest(batchId, payerIdentifier, currency, totalBatchAmount);
            HttpStatus httpStatus = invokeBatchAuthorizationApi(batchId, requestPayload, clientCorrelationId);

            variables.put(AUTHORIZATION_ACCEPTED, httpStatus.is2xxSuccessful());
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

    private HttpStatus invokeBatchAuthorizationApi(String batchId, AuthorizationRequest requestPayload, String clientCorrelationId) {
        RestTemplate restTemplate = new RestTemplate();
        AuthorizationResponse authResponse = null;
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Client-Correlation-ID", clientCorrelationId);

        HttpEntity<AuthorizationRequest> requestEntity = new HttpEntity<>(requestPayload, headers);
        String endpoint = "/batches/" + batchId;

        ResponseEntity<String> responseEntity = restTemplate.exchange(endpoint, HttpMethod.POST, requestEntity, String.class);
        return responseEntity.getStatusCode();
    }
}
