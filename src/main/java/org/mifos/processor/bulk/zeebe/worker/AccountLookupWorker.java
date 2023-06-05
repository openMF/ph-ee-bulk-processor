package org.mifos.processor.bulk.zeebe.worker;
import com.amazonaws.services.dynamodbv2.xspec.S;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.connector.common.mojaloop.dto.Party;
import org.mifos.connector.common.mojaloop.dto.PartyIdInfo;
import org.mifos.connector.common.mojaloop.type.IdentifierType;
import org.mifos.processor.bulk.camel.config.CamelProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;
import static org.mifos.processor.bulk.zeebe.worker.Worker.ACCOUNT_LOOKUP;
import static org.mifos.processor.bulk.zeebe.worker.Worker.ACCOUNT_LOOKUP_CALLBACK;

@Component
public class AccountLookupWorker extends BaseWorker{
    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    private ProducerTemplate producerTemplate;
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${identity-account-mapper.hostname}")
    private String identityMapperURL;
    @Value("${bulk-processor.hostname}")
    private String bulkURL;
    @Override
    public void setup() {
        logger.info("## generating " + ACCOUNT_LOOKUP + "zeebe worker");
        zeebeClient.newWorker()
                .jobType(ACCOUNT_LOOKUP.getValue())
                .handler((client, job) -> {
                    logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
                    Map<String, Object> existingVariables = job.getVariablesAsMap();
                    existingVariables.put(ACCOUNT_LOOKUP_RETRY_COUNT, 1);
                    existingVariables.put(CACHED_TRANSACTION_ID, job.getKey());

                    boolean isTransactionRequest = (boolean) existingVariables.get(IS_RTP_REQUEST);
                    String tenantId = (String) existingVariables.get(TENANT_ID);
                    Object channelRequest = existingVariables.get(CHANNEL_REQUEST);
                    TransactionChannelRequestDTO request = objectMapper.readValue((String) channelRequest, TransactionChannelRequestDTO.class);

                    existingVariables.put(INITIATOR_FSP_ID, tenantId);
                    PartyIdInfo requestedParty = isTransactionRequest ? request.getPayer().getPartyIdInfo() : request.getPayee().getPartyIdInfo();

                    String payeeIdentity =  requestedParty.getPartyIdentifier();
                    String paymentModality = requestedParty.getPartyIdType().toString();


                    Exchange exchange = new DefaultExchange(camelContext);
                    exchange.setProperty(HOST, identityMapperURL);
                    exchange.setProperty(PAYEE_IDENTITY, payeeIdentity);
                    exchange.setProperty(PAYMENT_MODALITY, paymentModality);
                    exchange.setProperty(CALLBACK,bulkURL + "/accountLookup/Callback");
                    exchange.setProperty(TRANSACTION_ID, existingVariables.get(TRANSACTION_ID));
                    exchange.setProperty("requestId", job.getKey());
                    exchange.setProperty(CHANNEL_REQUEST, channelRequest);
                    exchange.setProperty(ORIGIN_DATE, existingVariables.get(ORIGIN_DATE));
                    exchange.setProperty(TENANT_ID, tenantId);
                    producerTemplate.send("direct:send-account-lookup", exchange);


                    client.newCompleteCommand(job.getKey())
                            .variables(existingVariables)
                            .send()
                    ;
                })
                .name(String.valueOf(ACCOUNT_LOOKUP))
                .open();

    }
}
