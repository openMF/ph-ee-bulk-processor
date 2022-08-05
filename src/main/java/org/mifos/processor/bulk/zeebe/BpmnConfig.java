package org.mifos.processor.bulk.zeebe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BpmnConfig {

    @Value("${bpmn.flows.slcb}")
    public String slcbBpmn;

}
