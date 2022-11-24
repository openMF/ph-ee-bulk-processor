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

        Map<String, Object> variables = exchange.getProperty(ZEEBE_VARIABLE, Map.class);
        variables.put(PAYMENT_MODE, "gsma");
        List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
        while(transactionList.size() > 0) {
            GSMATransaction gsmaTransaction = Utils.convertTxnToGSMA(transactionList.get(0));
            exchange.setProperty(GSMA_CHANNEL_REQUEST, gsmaTransaction);
            exchange.setProperty(INIT_SUB_BATCH_FAILED, false);
            transactionList.remove(0);
        }

        exchange.getIn().setHeader("Platform-TenantId", exchange.getProperty(TENANT_NAME));
        GSMATransaction gsmaTransaction = exchange.getProperty(GSMA_CHANNEL_REQUEST, GSMATransaction.class);
        exchange.getIn().setBody(gsmaTransaction);
    }
}
