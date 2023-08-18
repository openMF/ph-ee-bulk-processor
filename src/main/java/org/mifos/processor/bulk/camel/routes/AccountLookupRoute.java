package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.ORDERED_BY;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.mifos.connector.common.identityaccountmapper.dto.AccountMapperRequestDTO;
import org.mifos.connector.common.identityaccountmapper.dto.BeneficiaryDTO;
import org.mifos.processor.bulk.camel.processor.AccountLookupCallbackProcessor;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class AccountLookupRoute extends BaseRouteBuilder {

    @Autowired
    AccountLookupCallbackProcessor accountLookupCallbackProcessor;

    @Override
    public void configure() throws Exception {

        from("rest:POST:/accountLookup/Callback").log(LoggingLevel.DEBUG, "######## -> ACCOUNT LOOKUP CALLBACK")
                .process(accountLookupCallbackProcessor).setBody(constant("Received")).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .end();

        from("direct:send-account-lookup").id("send-account-lookup").process(exchange -> {
            String callbackUrl = exchange.getProperty(CALLBACK, String.class);
            exchange.getIn().setHeader(CALLBACK, callbackUrl);
        }).setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader(Exchange.HTTP_QUERY,
                        simple(new StringBuilder().append(PAYEE_IDENTITY).append("=${exchangeProperty.").append(PAYEE_IDENTITY).append("}&")
                                .append(PAYMENT_MODALITY).append("=${exchangeProperty.").append(PAYMENT_MODALITY).append("}&")
                                .append("requestId=${exchangeProperty.requestId}").toString()))
                .setProperty(HOST, simple("{{identity-account-mapper.hostname}}"))
                .setProperty(ENDPOINT, simple("identityAccountMapper/accountLookup/")).to("direct:external-api-calling");

        from("rest:POST:/batchAccountLookup/Callback").log(LoggingLevel.DEBUG, "######## ->BATCH ACCOUNT LOOKUP CALLBACK")
                .process(accountLookupCallbackProcessor).setBody(constant("Received")).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .end();

        from(RouteId.ACCOUNT_LOOKUP.getValue()).id(RouteId.ACCOUNT_LOOKUP.getValue()).log("Starting route " + RouteId.ACCOUNT_LOOKUP.name())
                .to("direct:download-file").to("direct:get-transaction-array").to("direct:batch-account-lookup").to("direct:update-file")
                .to("direct:upload-file").process(exchange -> {
                    exchange.setProperty(PARTY_LOOKUP_FAILED, false);
                });


        from("direct:batch-account-lookup").id("direct:batch-account-lookup").process(exchange -> {
                    List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
                    HashMap<String, List<Transaction>> stringListHashMap = new HashMap<>();
                    List<BeneficiaryDTO> beneficiaryDTOList = new ArrayList<>();
                    transactionList.forEach(transaction -> {
                        beneficiaryDTOList.add(new BeneficiaryDTO(transaction.getPayeeIdentifier(),"","",""));
                    });
                    String requestId = exchange.getProperty(REQUEST_ID, String.class);
                    String callbackUrl = exchange.getProperty(CALLBACK, String.class);
                    String registeringInstitutionId = exchange.getProperty(REGISTERING_INSTITUTION_ID, String.class);
                    AccountMapperRequestDTO accountMapperRequestDTO =  new AccountMapperRequestDTO(requestId,registeringInstitutionId, beneficiaryDTOList);
                    String requestBody = objectMapper.writeValueAsString(accountMapperRequestDTO);

                    exchange.getIn().setHeader(CALLBACK, callbackUrl);
                    exchange.getIn().setHeader(REGISTERING_INSTITUTION_ID, registeringInstitutionId);
                    exchange.getIn().setBody(requestBody);
                }).setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setProperty(HOST, simple("{{identity-account-mapper.hostname}}"))
                .setProperty(ENDPOINT, simple("/accountLookup")).to("direct:external-api-calling");
    }
}
