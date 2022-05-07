package org.mifos.processor.bulk.camel.routes;

import org.mifos.processor.bulk.zeebe.ZeebeProcessStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;

@Component
public class ProcessorStartRoute extends BaseRouteBuilder{

    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    @Value("${bpmn.flows.bulk-processor}")
    private String workflowId;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void configure() throws Exception {
        setup();
    }

    private void setup() {
        from("rest:POST:/bulk/transfer/{requestId}/{fileName}")
                .process(exchange -> {
                    String fileName = exchange.getIn().getHeader("fileName", String.class);
                    String requestId = exchange.getIn().getHeader("requestId", String.class);
                    String batchId = UUID.randomUUID().toString();

                    logger.info("\n\n Filename: " + fileName + " \n\n");
                    logger.info("\n\n BatchId: " + batchId + " \n\n");
                    
                    Map<String, Object> variables = new HashMap<>();
                    variables.put(BATCH_ID, batchId);
                    variables.put("fileName", fileName);
                    variables.put("requestId", requestId);

                    zeebeProcessStarter.startZeebeWorkflow(workflowId, "", variables);
                });
    }
}
