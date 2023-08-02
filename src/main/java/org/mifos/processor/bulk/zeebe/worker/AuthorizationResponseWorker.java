package org.mifos.processor.bulk.zeebe.worker;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationResponseWorker extends BaseWorker {

    private static final String AUTHORIZATION_ACCEPTED = "authorizationAccepted";

    private static final String APPROVED_AMOUNT = "approvedAmount";

    private static final String AUTHORIZATION_SUCCESSFUL = "authorizationSuccessful";

    @Override
    public void setup() {
        newWorker(Worker.AUTHORIZATION, ((client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            logger.info("Job key: {}", job.getKey());
            logger.info("Job key: {}", job.getProcessInstanceKey());

            Map<String, Object> variables = job.getVariablesAsMap();

            if ((boolean) variables.get(AUTHORIZATION_ACCEPTED)) {
                variables.put(AUTHORIZATION_SUCCESSFUL, "Y".equals(variables.get("authorizationStatus")));
                variables.put(APPROVED_AMOUNT, variables.get("partyLookupSuccessfulAmount"));
            }
            else {
                variables.put(AUTHORIZATION_SUCCESSFUL, false);
            }
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        }));
    }
}
