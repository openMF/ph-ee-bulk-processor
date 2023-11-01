package org.mifos.processor.bulk.camel.routes;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;

import static org.mifos.processor.bulk.camel.config.CamelProperties.*;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;

@Component
public class AccountLookupRoute extends BaseRouteBuilder {
    @Value("${identity_account_mapper.account_lookup}")
    private String accountLookupEndpoint;
    @Value("${identity_account_mapper.hostname}")
    private String identityURL;
    @Override
    public void configure() throws Exception {
        Processor disableSslProcessor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                // Disable SSL certificate validation
                HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);
            }
        };
        from("direct:send-account-lookup")
                .id("account-lookup")
                .process(exchange -> {
                    String callbackUrl = exchange.getProperty(CALLBACK, String.class);
                    String registeringInstitutionId = exchange.getProperty(HEADER_REGISTERING_INSTITUTE_ID, String.class);
                    exchange.getIn().setHeader(CALLBACK, callbackUrl);
                    exchange.getIn().setHeader(HEADER_REGISTERING_INSTITUTE_ID, registeringInstitutionId);
                })
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD(identityURL + accountLookupEndpoint + "?"
                        + PAYEE_IDENTITY + "=${exchangeProperty.payeeIdentity}&"
                        + PAYMENT_MODALITY + "=${exchangeProperty.paymentModality}&"
                        + "requestId=${exchangeProperty.requestId}")
                .log("API Response: ${body}")
                .process(disableSslProcessor);
    }
}
