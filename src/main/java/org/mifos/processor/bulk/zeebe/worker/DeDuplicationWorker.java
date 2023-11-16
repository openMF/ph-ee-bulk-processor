package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.DE_DUPLICATION_FAILED;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DeDuplicationWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.DE_DEPLICATION, (client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            logger.info("Started {} worker", Worker.DE_DEPLICATION.getValue());
            Map<String, Object> variables = job.getVariablesAsMap();

            variables.put(DE_DUPLICATION_FAILED, false);

            logger.info("Zeebe variables in dedup: {}", variables);
            client.newCompleteCommand(job.getKey()).variables(variables).send();
            logger.info("Completed {} worker", Worker.DE_DEPLICATION);
        });

    }
}
