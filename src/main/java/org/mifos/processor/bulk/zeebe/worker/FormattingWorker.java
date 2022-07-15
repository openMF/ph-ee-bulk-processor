package org.mifos.processor.bulk.zeebe.worker;

import java.util.Map;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FORMATTING_FAILED;

public class FormattingWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.FORMATTING, (client, job) -> {
            Map<String, Object> variables = job.getVariablesAsMap();
            if (workerConfig.isFormattingWorkerEnabled) {
                variables.put(FORMATTING_FAILED, false);
            }

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
