package org.mifos.processor.bulk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class OperationsAppConfig {

    @Value("${operations-app.contactpoint}")
    public String operationAppContactPoint;

    @Value("${operations-app.endpoints.batch-transaction}")
    public String batchTransactionEndpoint;

    public String batchTransactionUrl;

    @PostConstruct
    private void setup() {
        batchTransactionUrl = operationAppContactPoint + batchTransactionEndpoint;
    }
}
