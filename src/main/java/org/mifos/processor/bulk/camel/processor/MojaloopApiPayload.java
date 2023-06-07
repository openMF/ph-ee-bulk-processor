package org.mifos.processor.bulk.camel.processor;

import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST_ELEMENT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Function;
import org.apache.camel.Exchange;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.utility.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MojaloopApiPayload implements Function<Exchange, String> {

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public String apply(Exchange exchange) {
        Transaction transaction = exchange.getProperty(TRANSACTION_LIST_ELEMENT, Transaction.class);
        TransactionChannelRequestDTO inboundTransferPayload = Utils.convertTxnToInboundTransferPayload(transaction);
        try {
            return objectMapper.writeValueAsString(inboundTransferPayload);
        } catch (JsonProcessingException e) {
            throw new DTOJsonConversionException(MojaloopApiPayload.class);
        }
    }
}
