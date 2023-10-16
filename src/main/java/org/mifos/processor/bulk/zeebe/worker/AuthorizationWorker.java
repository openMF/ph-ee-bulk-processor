package org.mifos.processor.bulk.zeebe.worker;

import java.util.Map;
import org.mifos.processor.bulk.schema.AuthorizationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.AUTHORIZATION_SUCCESSFUL;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.APPROVED_AMOUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CLIENT_CORRELATION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.AUTHORIZATION_ACCEPTED;

@Component
public class AuthorizationWorker extends BaseWorker {

    @Value("${batch-authorization.callback-url}")
    private String callbackURLPath;

    @Value("${mock-payment-schema.contactpoint}")
    private String mockPaymentSchemaContactPoint;

    @Value("${mock-payment-schema.endpoints.authorization}")
    private String authorizationEndpoint;

    private static final String X_CLIENT_CORRELATION_ID = "X-Client-Correlation-ID";

    private static final String X_CALLBACK_URL = "X-CallbackURL";

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
            String clientCorrelationId = Long.toString(job.getKey());

            AuthorizationRequest requestPayload = new AuthorizationRequest(batchId, payerIdentifier, currency, totalBatchAmount);
            HttpStatus httpStatus = invokeBatchAuthorizationApi(batchId, requestPayload, clientCorrelationId);

            variables.put(APPROVED_AMOUNT, totalBatchAmount);
            variables.put(CLIENT_CORRELATION_ID, clientCorrelationId);
            variables.put(AUTHORIZATION_ACCEPTED, httpStatus.is2xxSuccessful());
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

    private HttpStatus invokeBatchAuthorizationApi(String batchId, AuthorizationRequest requestPayload, String clientCorrelationId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add(X_CLIENT_CORRELATION_ID, clientCorrelationId);
        headers.add(X_CALLBACK_URL, callbackURLPath);

        HttpEntity<AuthorizationRequest> requestEntity = new HttpEntity<>(requestPayload, headers);
        String endpoint = mockPaymentSchemaContactPoint + authorizationEndpoint + batchId;

        ResponseEntity<String> responseEntity = restTemplate.exchange(endpoint, HttpMethod.POST, requestEntity, String.class);
        return responseEntity.getStatusCode();
    }
}
