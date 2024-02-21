package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.HEADER_REGISTERING_INSTITUTE_ID;
import static org.mifos.processor.bulk.camel.config.CamelProperties.OVERRIDE_HEADER;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PAYEE_IDENTITY;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PAYMENT_MODALITY;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PARTY_LOOKUP_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REGISTERING_INSTITUTION_ID;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.REQUEST_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.mifos.connector.common.identityaccountmapper.dto.AccountMapperRequestDTO;
import org.mifos.connector.common.identityaccountmapper.dto.BeneficiaryDTO;
import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.service.BatchAccountLookup;
import org.mifos.processor.bulk.service.FileProcessingRouteService;
import org.mifos.processor.bulk.service.FileRouteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccountLookupRoute extends BaseRouteBuilder {

    @Value("${identity_account_mapper.account_lookup}")
    private String accountLookupEndpoint;
    @Value("${identity_account_mapper.hostname}")
    private String identityURL;
    @Value("${identity_account_mapper.hostname}")
    private String identityMapperURL;
    @Value("${identity_account_mapper.batch_account_lookup_callback}")
    private String batchAccountLookupCallback;
    @Value("${identity_account_mapper.batch_account_lookup}")
    private String batchAccountLookup;

    @Override
    public void configure() throws Exception {
        Processor disableSslProcessor = new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {
                // Disable SSL certificate validation
                HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);
            }
        };
        from("direct:send-account-lookup").id("account-lookup").process(exchange -> {
            String callbackUrl = exchange.getProperty(CALLBACK, String.class);
            String registeringInstitutionId = exchange.getProperty(HEADER_REGISTERING_INSTITUTE_ID, String.class);
            exchange.getIn().setHeader(CALLBACK, callbackUrl);
            exchange.getIn().setHeader(HEADER_REGISTERING_INSTITUTE_ID, registeringInstitutionId);
        }).setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD(identityURL + accountLookupEndpoint + "?" + PAYEE_IDENTITY + "=${exchangeProperty.payeeIdentity}&" + PAYMENT_MODALITY
                        + "=${exchangeProperty.paymentModality}&" + "requestId=${exchangeProperty.requestId}")
                .log("API Response: ${body}").process(disableSslProcessor);

        from(RouteId.ACCOUNT_LOOKUP.getValue()).id(RouteId.ACCOUNT_LOOKUP.getValue()).log("Starting route " + RouteId.ACCOUNT_LOOKUP.name())
                .process(exchange -> exchange.setProperty(OVERRIDE_HEADER, true)).bean(FileRouteService.class, "downloadFile")
                .bean(FileProcessingRouteService.class, "getTxnArray").bean(BatchAccountLookup.class, "doBatchAccountLookup")
                .bean(FileProcessingRouteService.class, "updateFile").bean(FileRouteService.class, "uploadFile").process(exchange -> {
                    exchange.setProperty(PARTY_LOOKUP_FAILED, false);
                });

        from("direct:batch-account-lookup").id("direct:batch-account-lookup").process(exchange -> {
            List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
            HashMap<String, List<Transaction>> stringListHashMap = new HashMap<>();
            List<BeneficiaryDTO> beneficiaryDTOList = new ArrayList<>();
            transactionList.forEach(transaction -> {
                beneficiaryDTOList.add(new BeneficiaryDTO(transaction.getPayeeIdentifier(), "", "", ""));
            });
            String requestId = exchange.getProperty(REQUEST_ID, String.class);
            String callbackUrl = exchange.getProperty(CALLBACK, String.class);
            String registeringInstitutionId = exchange.getProperty(HEADER_REGISTERING_INSTITUTE_ID, String.class);
            AccountMapperRequestDTO accountMapperRequestDTO = new AccountMapperRequestDTO(requestId, registeringInstitutionId,
                    beneficiaryDTOList);
            String requestBody = objectMapper.writeValueAsString(accountMapperRequestDTO);

            exchange.getIn().setHeader(CALLBACK, callbackUrl);
            exchange.getIn().setHeader(REGISTERING_INSTITUTION_ID, registeringInstitutionId);
            exchange.getIn().setHeader("Content-type", "application/json");
            exchange.getIn().setBody(requestBody);
        }).setHeader(Exchange.HTTP_METHOD, constant("POST")).toD(identityURL + batchAccountLookup).log("API Response: ${body}")
                .process(disableSslProcessor);

    }
}
