package org.mifos.processor.bulk.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.utility.Utils;
import org.springframework.stereotype.Component;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;

@Component
public class MojaloopApiProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        Transaction transaction = exchange.getProperty(TRANSACTION_LIST_ELEMENT, Transaction.class);
        TransactionChannelRequestDTO inboundTransferPayload = Utils.convertTxnToInboundTransferPayload(transaction);
        exchange.getIn().setBody(inboundTransferPayload);
    }
}
