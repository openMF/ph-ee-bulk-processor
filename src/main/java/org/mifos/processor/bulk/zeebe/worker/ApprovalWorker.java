package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.APPROVAL_FAILED;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ApprovalWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.APPROVAL, (client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();

            if (workerConfig.isApprovalWorkerEnabled) {
                variables.put(APPROVAL_FAILED, false);
            }

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
