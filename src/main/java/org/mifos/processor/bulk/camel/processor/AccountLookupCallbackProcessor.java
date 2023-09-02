package org.mifos.processor.bulk.camel.processor;

import static org.mifos.processor.bulk.camel.config.CamelProperties.CACHED_TRANSACTION_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PAYEE_PARTY_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PAYEE_PARTY_ID_TYPE;
import static org.mifos.processor.bulk.zeebe.ZeebeMessages.ACCOUNT_LOOKUP;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ACCOUNT_LOOKUP_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_LOOKUP_FSP_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.mifos.processor.bulk.schema.AccountLookupResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountLookupCallbackProcessor implements Processor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired(required = false)
    private ZeebeClient zeebeClient;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        Map<String, Object> variables = new HashMap<>();

        String error = null;
        String response = exchange.getIn().getBody(String.class);
        AccountLookupResponseDTO accountLookupResponseDTO = null;
        try {
            accountLookupResponseDTO = objectMapper.readValue(response, AccountLookupResponseDTO.class);
            variables.put(ACCOUNT_LOOKUP_FAILED, false);
            variables.put(PAYEE_PARTY_ID, accountLookupResponseDTO.getPaymentModalityList().get(0).getFinancialAddress());
            variables.put(PAYEE_PARTY_ID_TYPE, accountLookupResponseDTO.getPaymentModalityList().get(0).getPaymentModality());
            variables.put(PARTY_LOOKUP_FSP_ID, accountLookupResponseDTO.getPaymentModalityList().get(0).getBankingInstitutionCode());
            exchange.setProperty(CACHED_TRANSACTION_ID, accountLookupResponseDTO.getRequestId());
        } catch (IOException e) {
            variables.put(ACCOUNT_LOOKUP_FAILED, true);
            error = objectMapper.readValue(response, String.class);
        }

        if (zeebeClient != null) {

            zeebeClient.newPublishMessageCommand().messageName(ACCOUNT_LOOKUP)
                    .correlationKey(exchange.getProperty(CACHED_TRANSACTION_ID, String.class)).timeToLive(Duration.ofMillis(50000))
                    .variables(variables).send();
        }
    }
}
