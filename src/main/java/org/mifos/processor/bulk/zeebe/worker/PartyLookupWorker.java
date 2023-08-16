package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_LOOKUP_FAILED;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PartyLookupWorker extends BaseWorker {

    @Override
    public void setup() {
        newWorker(Worker.PARTY_LOOKUP, (client, job) -> {
            logger.debug("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = job.getVariablesAsMap();

            if (workerConfig.isPartyLookUpWorkerEnabled) {
                variables.put(PARTY_LOOKUP_FAILED, false);
            }

            client.newCompleteCommand(job.getKey()).variables(variables).send();
        });
    }

}
