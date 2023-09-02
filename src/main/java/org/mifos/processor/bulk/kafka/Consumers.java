package org.mifos.processor.bulk.kafka;

import static org.mifos.connector.common.mojaloop.type.InitiatorType.CONSUMER;
import static org.mifos.connector.common.mojaloop.type.Scenario.TRANSFER;
import static org.mifos.connector.common.mojaloop.type.TransactionRole.PAYER;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.BATCH_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.GSMA_CHANNEL_REQUEST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.INITIATOR_FSPID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.IS_RTP_REQUEST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_ID_TYPE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_LOOKUP_FSPID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TRANSACTION_TYPE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.connector.common.gsma.dto.GSMATransaction;
import org.mifos.connector.common.gsma.dto.GsmaParty;
import org.mifos.connector.common.mojaloop.dto.MoneyData;
import org.mifos.connector.common.mojaloop.dto.Party;
import org.mifos.connector.common.mojaloop.dto.PartyIdInfo;
import org.mifos.connector.common.mojaloop.dto.TransactionType;
import org.mifos.connector.common.mojaloop.type.IdentifierType;
import org.mifos.processor.bulk.schema.TransactionOlder;
import org.mifos.processor.bulk.zeebe.ZeebeProcessStarter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Consumers {

    @Value("${bpmn.flows.international-remittance-payer}")
    private String internationalRemittancePayer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ZeebeProcessStarter zeebeProcessStarter;

    @KafkaListener(topics = "${kafka.topic.gsma.name}", groupId = "group_id")
    public void listenTopicGsma(String message) throws JsonProcessingException {
        log.debug("Received Message in topic GSMA and group group_id: {}", message);
        TransactionOlder transaction = objectMapper.readValue((String) message, TransactionOlder.class);
        String tenantId = "ibank-usa";

        GSMATransaction gsmaChannelRequest = new GSMATransaction();
        gsmaChannelRequest.setAmount(transaction.getAmount());
        gsmaChannelRequest.setCurrency(transaction.getCurrency());
        gsmaChannelRequest.setRequestingLei("ibank-usa");
        gsmaChannelRequest.setReceivingLei("ibank-india");
        GsmaParty creditParty = new GsmaParty();
        creditParty.setKey("msisdn");
        creditParty.setValue(transaction.getAccountNumber());
        GsmaParty debitParty = new GsmaParty();
        debitParty.setKey("msisdn");
        debitParty.setValue(transaction.getAccountNumber());
        gsmaChannelRequest.setCreditParty(new GsmaParty[] { creditParty });
        gsmaChannelRequest.setDebitParty(new GsmaParty[] { debitParty });
        // gsmaChannelRequest.setInternationalTransferInformation().setReceivingAmount(gsmaChannelRequest.getAmount());

        TransactionChannelRequestDTO channelRequest = new TransactionChannelRequestDTO(); // Fineract Object
        Party payee = new Party(new PartyIdInfo(IdentifierType.MSISDN, transaction.getAccountNumber()));
        Party payer = new Party(new PartyIdInfo(IdentifierType.MSISDN, "7543010"));

        MoneyData moneyData = new MoneyData();
        moneyData.setAmount(transaction.getAmount());
        moneyData.setCurrency(transaction.getCurrency());

        channelRequest.setPayer(payer);
        channelRequest.setPayee(payee);
        channelRequest.setAmount(moneyData);

        TransactionType transactionType = new TransactionType();
        transactionType.setInitiator(PAYER);
        transactionType.setInitiatorType(CONSUMER);
        transactionType.setScenario(TRANSFER);

        Map<String, Object> extraVariables = new HashMap<>();
        extraVariables.put(IS_RTP_REQUEST, false);
        extraVariables.put(TRANSACTION_TYPE, "inttransfer");
        extraVariables.put(TENANT_ID, tenantId);

        extraVariables.put(BATCH_ID, transaction.getBatchId());

        String tenantSpecificBpmn = internationalRemittancePayer.replace("{dfspid}", tenantId);
        channelRequest.setTransactionType(transactionType);

        PartyIdInfo requestedParty = (boolean) extraVariables.get(IS_RTP_REQUEST) ? channelRequest.getPayer().getPartyIdInfo()
                : channelRequest.getPayee().getPartyIdInfo();
        extraVariables.put(PARTY_ID_TYPE, requestedParty.getPartyIdType());
        extraVariables.put(PARTY_ID, requestedParty.getPartyIdentifier());

        extraVariables.put(GSMA_CHANNEL_REQUEST, objectMapper.writeValueAsString(gsmaChannelRequest));
        extraVariables.put(PARTY_LOOKUP_FSPID, gsmaChannelRequest.getReceivingLei());
        extraVariables.put(INITIATOR_FSPID, gsmaChannelRequest.getRequestingLei());

        String transactionId = zeebeProcessStarter.startZeebeWorkflow(tenantSpecificBpmn, objectMapper.writeValueAsString(channelRequest),
                extraVariables);

        log.debug("GSMA Transaction Started with:{} ", transactionId);
    }

    @KafkaListener(topics = "${kafka.topic.slcb.name}", groupId = "group_id")
    public void listenTopicSlcb(String message) {
        log.debug("Received Message in topic SLCB and group group_id:{} ", message);
    }
}
