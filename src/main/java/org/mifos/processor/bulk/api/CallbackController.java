package org.mifos.processor.bulk.api;

import static org.mifos.processor.bulk.zeebe.ZeebeMessages.AUTHORIZATION_RESPONSE;

import io.camunda.zeebe.client.ZeebeClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.mifos.processor.bulk.schema.AuthorizationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CallbackController {

    @Autowired
    private ZeebeClient zeebeClient;

    @PostMapping("/authorization/callback")
    public ResponseEntity<Object> handleAuthorizationCallback(@RequestBody AuthorizationResponse authResponse) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("clientCorrelationId", authResponse.getClientCorrelationId());
        variables.put("authorizationStatus", authResponse.getStatus());
        variables.put("authorizationFailReason", authResponse.getReason());

        if (zeebeClient != null) {
            zeebeClient.newPublishMessageCommand()
                    .messageName(AUTHORIZATION_RESPONSE)
                    .correlationKey(authResponse.getClientCorrelationId())
                    .timeToLive(Duration.ofMillis(50000))
                    .variables(variables).send();
        }

        return ResponseEntity.ok().build();
    }

}
