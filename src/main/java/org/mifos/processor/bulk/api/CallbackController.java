package org.mifos.processor.bulk.api;

import io.camunda.zeebe.client.ZeebeClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.mifos.processor.bulk.schema.AuthorizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;

@RestController
public class CallbackController {

    @Autowired
    private ZeebeClient zeebeClient;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String EXPECTED_AUTH_STATUS = "Y";


    @PostMapping("/authorization/callback")
    public ResponseEntity<Object> handleAuthorizationCallback(@RequestBody AuthorizationResponse authResponse) {
        Map<String, Object> variables = new HashMap<>();

        boolean isAuthorizationSuccessful = EXPECTED_AUTH_STATUS.equals(authResponse.getStatus());
        variables.put(AUTHORIZATION_SUCCESSFUL, isAuthorizationSuccessful);
        variables.put(CLIENT_CORRELATION_ID, authResponse.getClientCorrelationId());
        variables.put(AUTHORIZATION_STATUS, authResponse.getStatus());
        variables.put(AUTHORIZATION_FAIL_REASON, authResponse.getReason());

        if (!isAuthorizationSuccessful) {
            variables.put(APPROVED_AMOUNT, 0);
        }

        if (zeebeClient != null) {
            zeebeClient.newPublishMessageCommand()
                    .messageName(AUTHORIZATION_RESPONSE)
                    .correlationKey(authResponse.getClientCorrelationId())
                    .timeToLive(Duration.ofMillis(500000))
                    .variables(variables).send();
        }
        return ResponseEntity.ok().build();
    }

}
