package org.mifos.processor.bulk.zeebe.worker;

import java.util.Map;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MERGE_FAILED;

public class MergeBackWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.MERGE_BACK, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();
            if (workerConfig.isMergeBackWorkerEnabled) {
                variables.put(MERGE_FAILED, false);
            }

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
