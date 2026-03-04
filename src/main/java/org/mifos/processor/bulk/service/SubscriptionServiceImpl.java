package org.mifos.processor.bulk.service;

import static org.mifos.processor.bulk.camel.config.CamelProperties.EVENT_TYPE;

import javax.annotation.PostConstruct;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final ProducerTemplate producerTemplate;

    protected final CamelContext camelContext;

    public String eventType;

    public SubscriptionServiceImpl(ProducerTemplate producerTemplate, CamelContext camelContext) {
        this.producerTemplate = producerTemplate;
        this.camelContext = camelContext;
    }

    @PostConstruct
    @Override
    public void subscribeToEvent() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(EVENT_TYPE, eventType);
        producerTemplate.send("direct:subscribe", exchange);
    }

}
