package org.mifos.processor.bulk.zeebe.worker;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationResponseWorker extends BaseWorker {

    private static final String AUTHORIZATION_ACCEPTED = "authorizationAccepted";

    @Override
    public void setup() {
        newWorker(Worker.AUTHORIZATION, ((client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            logger.info("Job key: {}", job.getKey());
            logger.info("Job key: {}", job.getProcessInstanceKey());

            Map<String, Object> variables = job.getVariablesAsMap();

            if ((boolean) variables.get(AUTHORIZATION_ACCEPTED)) {
                variables.put("authorizationSuccessful", "Y".equals(variables.get("authorizationStatus")));
            }
            else {
                variables.put("authorizationSuccessful", false);
            }
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        }));
    }
}
