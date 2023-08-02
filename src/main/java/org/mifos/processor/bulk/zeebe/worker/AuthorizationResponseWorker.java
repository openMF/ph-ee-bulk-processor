package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.APPROVED_AMOUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.AUTHORIZATION_ACCEPTED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.AUTHORIZATION_SUCCESSFUL;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationResponseWorker extends BaseWorker {

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
            } else {
                variables.put(AUTHORIZATION_SUCCESSFUL, false);
            }
            client.newCompleteCommand(job.getKey()).variables(variables).send();
        }));
    }
}
