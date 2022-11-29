package org.mifos.processor.bulk.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.mifos.connector.common.gsma.dto.GSMATransaction;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.utility.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.INIT_SUB_BATCH_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYMENT_MODE;

@Component
public class GsmaApiProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        Transaction transaction = exchange.getProperty(TRANSACTION_LIST_ELEMENT, Transaction.class);
        exchange.getIn().setHeader("Platform-TenantId", exchange.getProperty(TENANT_NAME));
        GSMATransaction gsmaTransaction = Utils.convertTxnToGSMA(transaction);
        exchange.getIn().setBody(gsmaTransaction);
    }
}
