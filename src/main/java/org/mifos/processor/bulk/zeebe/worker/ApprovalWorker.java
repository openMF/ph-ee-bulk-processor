package org.mifos.processor.bulk.zeebe.worker;

import org.springframework.stereotype.Component;

import java.util.Map;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.APPROVAL_FAILED;

@Component
public class ApprovalWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.APPROVAL, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();

            if (workerConfig.isApprovalWorkerEnabled) {
                variables.put(APPROVAL_FAILED, false);
            }

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
