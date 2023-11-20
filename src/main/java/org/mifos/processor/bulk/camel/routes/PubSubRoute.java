package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.ENDPOINT;
import static org.mifos.processor.bulk.camel.config.CamelProperties.EVENT_TYPE;
import static org.mifos.processor.bulk.camel.config.CamelProperties.HOST;

import org.apache.camel.Exchange;
import org.mifos.processor.bulk.config.PubSubConfig;
import org.mifos.processor.bulk.config.SecurityServerConfig;
import org.mifos.processor.bulk.schema.SubscriptionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubRoute extends BaseRouteBuilder {

    private final PubSubConfig pubSubConfig;
    private final SecurityServerConfig securityServerConfig;

    @Value("${gov-stack-client.header-key}")
    private String govStackClientHeaderKey;

    @Value("${gov-stack-client.header-value}")
    private String govStackClientHeaderValue;

    public PubSubRoute(PubSubConfig pubSubConfig, SecurityServerConfig securityServerConfig) {
        this.pubSubConfig = pubSubConfig;
        this.securityServerConfig = securityServerConfig;
    }

    @Override
    public void configure() throws Exception {

        // needs EVENT_TYPE input from exchange
        from("direct:subscribe").id("direct:subscribe")
                .setBody(exchange -> getEventTypeSpecificSubscriptionDTO(exchange.getProperty(EVENT_TYPE, String.class)))
                .setHeader(Exchange.HTTP_METHOD, constant(HttpRequestMethod.POST.toString()))
                .setHeader(govStackClientHeaderKey, constant(govStackClientHeaderValue))
                .setProperty(HOST, constant(securityServerConfig.host)).setProperty(ENDPOINT, constant(securityServerConfig.subscribingUrl))
                .to("direct:external-api-calling");

    }

    private SubscriptionDTO getDefaultSubscriptionDTO() {
        return SubscriptionDTO.subscriptionDTOBuilder.roomCode(pubSubConfig.roomCode).roomClass(pubSubConfig.roomClass)
                .srcOperationId("bulkProcessing").srcServiceCode("bulk").dstOperationId("newRecord").dstServiceCode("bulk").build();
    }

    private SubscriptionDTO getEventTypeSpecificSubscriptionDTO(String eventType) {
        SubscriptionDTO subscriptionDTO = getDefaultSubscriptionDTO();
        subscriptionDTO.setDstServiceCode(eventType); // todo update once confirmed which field is for eventType
        return subscriptionDTO;
    }
}
