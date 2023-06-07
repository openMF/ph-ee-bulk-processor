package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.ENDPOINT;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HOST;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PAYEE_IDENTITY;
import static org.mifos.processor.bulk.camel.config.CamelProperties.PAYMENT_MODALITY;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.CALLBACK;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.mifos.processor.bulk.camel.processor.AccountLookupCallbackProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountLookupRoute extends BaseRouteBuilder {

    @Autowired
    AccountLookupCallbackProcessor accountLookupCallbackProcessor;

    @Override
    public void configure() throws Exception {

        from("rest:POST:/accountLookup/Callback").log(LoggingLevel.DEBUG, "######## -> ACCOUNT LOOKUP CALLBACK")
                .process(accountLookupCallbackProcessor).setBody(constant("Received")).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .end();

        from("direct:send-account-lookup").id("account-lookup").process(exchange -> {
            String callbackUrl = exchange.getProperty(CALLBACK, String.class);
            exchange.getIn().setHeader(CALLBACK, callbackUrl);
        }).setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader(Exchange.HTTP_QUERY,
                        simple(new StringBuilder().append(PAYEE_IDENTITY).append("=${exchangeProperty.").append(PAYEE_IDENTITY).append("}&")
                                .append(PAYMENT_MODALITY).append("=${exchangeProperty.").append(PAYMENT_MODALITY).append("}&")
                                .append("requestId=${exchangeProperty.requestId}").toString()))
                .setProperty(HOST, simple("{{identity-account-mapper.hostname}}"))
                .setProperty(ENDPOINT, simple("identityAccountMapper/accountLookup/")).to("direct:external-api-calling");
    }
}
