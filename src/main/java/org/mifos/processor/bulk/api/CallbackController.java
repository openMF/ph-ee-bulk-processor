package org.mifos.processor.bulk.api;

import io.camunda.zeebe.client.ZeebeClient;
import org.mifos.processor.bulk.schema.AuthorizationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.mifos.processor.bulk.camel.config.CamelProperties.CACHED_TRANSACTION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeMessages.ACCOUNT_LOOKUP;
import static org.mifos.processor.bulk.zeebe.ZeebeMessages.AUTHORIZATION_RESPONSE;

@RestController
public class CallbackController {

    @Autowired(required = false)
    private ZeebeClient zeebeClient;

    @PostMapping("/authorization/callback")
    public ResponseEntity<Object> handleAuthorizationCallback(@RequestBody AuthorizationResponse authResponse) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("clientCorrelationId", authResponse.getClientCorrelationId());
        variables.put("authorizationSuccessful", "Y".equals(authResponse.getStatus()));
        variables.put("authorizationFailReason", authResponse.getReason());

        if(zeebeClient != null){
            zeebeClient.newPublishMessageCommand()
                    .messageName(AUTHORIZATION_RESPONSE)    // update messageName
                    .correlationKey(authResponse.getClientCorrelationId())  // what is correlation key?
                    .timeToLive(Duration.ofMillis(50000))
                    .variables(variables).send();
        }

        return ResponseEntity.ok().build();
    }

}
