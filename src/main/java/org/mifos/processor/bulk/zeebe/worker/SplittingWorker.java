package org.mifos.processor.bulk.zeebe.worker;

import java.util.Map;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.MERGE_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.SPLITTING_FAILED;

public class SplittingWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.SPLITTING, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();
            if (workerConfig.isSplittingWorkerEnabled) {
                variables.put(SPLITTING_FAILED, false);
            }

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
