package org.mifos.processor.bulk.config;

import org.apache.camel.Processor;
import org.mifos.processor.bulk.camel.processor.GsmaApiProcessor;
import org.mifos.processor.bulk.camel.processor.MojaloopApiProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.Map;

@Component
public class ExternalApiProcessor {

    private Map<String, Processor> processorMap;

    @Autowired
    GsmaApiProcessor gsmaApiProcessor;

    @Autowired
    MojaloopApiProcessor mojaloopApiProcessor;

    @Autowired
    PaymentModeConfiguration paymentModeConfiguration;

    @PostConstruct
    private void registerApiProcessor() {
        for (PaymentModeMapping paymentMode: paymentModeConfiguration.getMappings()) {
            if (paymentMode.getId().equalsIgnoreCase("gsma")) {
                processorMap.put(paymentMode.getId(), gsmaApiProcessor);
            } else if (paymentMode.getId().equalsIgnoreCase("mojaloop")) {
                processorMap.put(paymentMode.getId(), mojaloopApiProcessor);
            }
        }
    }

    public Processor getApiProcessor(String id) {
        return processorMap.get(id);
    }

}
