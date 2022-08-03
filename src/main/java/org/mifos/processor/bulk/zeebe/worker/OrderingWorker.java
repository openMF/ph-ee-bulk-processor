package org.mifos.processor.bulk.zeebe.worker;

import java.util.Map;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ORDERING_FAILED;

public class OrderingWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.ORDERING, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();
            if (workerConfig.isOrderingWorkerEnabled) {
                variables.put(ORDERING_FAILED, false);
            }

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
