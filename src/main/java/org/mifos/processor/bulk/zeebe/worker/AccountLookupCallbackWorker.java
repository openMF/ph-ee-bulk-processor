package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.camel.config.CamelProperties.PAYEE_PARTY_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CHANNEL_REQUEST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ORIGIN_CHANNEL_REQUEST;
import static org.mifos.processor.bulk.zeebe.worker.Worker.ACCOUNT_LOOKUP_CALLBACK;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.connector.common.mojaloop.dto.Party;
import org.mifos.connector.common.mojaloop.dto.PartyIdInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountLookupCallbackWorker extends BaseWorker {

    @Autowired
    private ZeebeClient zeebeClient;
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void setup() {
        logger.info("## generating " + ACCOUNT_LOOKUP_CALLBACK + "zeebe worker");
        zeebeClient.newWorker().jobType(ACCOUNT_LOOKUP_CALLBACK.getValue()).handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> existingVariables = job.getVariablesAsMap();
            existingVariables.put(ORIGIN_CHANNEL_REQUEST, existingVariables.get(CHANNEL_REQUEST));
            TransactionChannelRequestDTO transactionChannelRequestDTO = objectMapper
                    .readValue((String) existingVariables.get(CHANNEL_REQUEST), TransactionChannelRequestDTO.class);
            String payeeId = existingVariables.get(PAYEE_PARTY_ID).toString();
            PartyIdInfo partyIdInfo = new PartyIdInfo(transactionChannelRequestDTO.getPayee().getPartyIdInfo().getPartyIdType(), payeeId);
            Party payee = new Party(partyIdInfo);
            transactionChannelRequestDTO.setPayee(payee);
            existingVariables.put(CHANNEL_REQUEST, objectMapper.writeValueAsString(transactionChannelRequestDTO));
            client.newCompleteCommand(job.getKey()).variables(existingVariables).send().join();
        }).name(ACCOUNT_LOOKUP_CALLBACK.getValue()).open();
    }
}
