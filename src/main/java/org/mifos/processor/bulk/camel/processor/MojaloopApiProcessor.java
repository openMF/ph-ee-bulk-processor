package org.mifos.processor.bulk.camel.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.utility.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;

@Component
public class MojaloopApiProcessor implements Processor {

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        List<String> payloads = new ArrayList<>();
        List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
        for (Transaction transaction : transactionList) {
            TransactionChannelRequestDTO requestDTO = Utils.convertTxnToInboundTransferPayload(transaction);
            payloads.add(objectMapper.writeValueAsString(requestDTO));
        }

        exchange.setProperty(PAYLOAD_LIST, payloads);
        exchange.getIn().setHeader("Platform-TenantId", exchange.getProperty(TENANT_NAME));
    }
}
