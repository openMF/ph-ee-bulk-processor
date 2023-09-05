package org.mifos.processor.bulk.zeebe.worker;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ACCOUNT_LOOKUP_RETRY_COUNT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CHANNEL_REQUEST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.INITIATOR_FSP_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.IS_RTP_REQUEST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ORIGIN_DATE;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TENANT_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.TRANSACTION_ID;
import static org.mifos.processor.bulk.zeebe.worker.Worker.ACCOUNT_LOOKUP;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.connector.common.mojaloop.dto.PartyIdInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccountLookupWorker extends BaseWorker {

    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    private ProducerTemplate producerTemplate;
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${identity_account_mapper.hostname}")
    private String identityMapperURL;
    @Value("${bulk_processor.hostname}")
    private String bulkURL;

    @Override
    public void setup() {
        logger.info("## generating " + ACCOUNT_LOOKUP + "zeebe worker");
        zeebeClient.newWorker().jobType(ACCOUNT_LOOKUP.getValue()).handler((client, job) -> {
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

            String payeeIdentity = requestedParty.getPartyIdentifier();
            String paymentModality = requestedParty.getPartyIdType().toString();

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(HOST, identityMapperURL);
            exchange.setProperty(PAYEE_IDENTITY, payeeIdentity);
            exchange.setProperty(PAYMENT_MODALITY, paymentModality);
            exchange.setProperty(CALLBACK,bulkURL + "/accountLookup/Callback" );
            exchange.setProperty(TRANSACTION_ID, existingVariables.get(TRANSACTION_ID));
            exchange.setProperty("requestId", job.getKey());
            exchange.setProperty(CHANNEL_REQUEST, channelRequest);
            exchange.setProperty(ORIGIN_DATE, existingVariables.get(ORIGIN_DATE));
            exchange.setProperty(TENANT_ID, tenantId);
            exchange.setProperty(HEADER_REGISTERING_INSTITUTE_ID, existingVariables.get(HEADER_REGISTERING_INSTITUTE_ID));
            logger.info("IAM Base {}", identityMapperURL);
            logger.info("Bulk Base {}", bulkURL);
            logger.info("RID {}", existingVariables.get(HEADER_REGISTERING_INSTITUTE_ID).toString());
            RequestSpecification requestSpec = new RequestSpecBuilder().build();
            requestSpec.relaxedHTTPSValidation();

            Response response = RestAssured.given(requestSpec)
                    .baseUri(identityMapperURL)
                    .header("X-CallbackURL",bulkURL + "/accountLookup/Callback" )
                    .header("registeringInstitutionId", existingVariables.get(HEADER_REGISTERING_INSTITUTE_ID).toString())
                    .queryParam("requestId", job.getKey())
                    .queryParam("payeeIdentity", payeeIdentity)
                    .queryParam("paymentModality", paymentModality)
                    .when()
                    .get("/beneficiary")
                    .andReturn();
            //producerTemplate.send("direct:send-account-lookup", exchange);

            client.newCompleteCommand(job.getKey()).variables(existingVariables).send();
        }).name(String.valueOf(ACCOUNT_LOOKUP)).open();

    }
}
