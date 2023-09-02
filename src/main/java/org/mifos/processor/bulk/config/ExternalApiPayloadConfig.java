package org.mifos.processor.bulk.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import lombok.Getter;
import org.apache.camel.Exchange;
import org.mifos.processor.bulk.camel.processor.GsmaApiPayload;
import org.mifos.processor.bulk.camel.processor.MojaloopApiPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ExternalApiPayloadConfig {

    private Map<String, Function<Exchange, String>> payloadMap = new HashMap<>();

    @Autowired
    GsmaApiPayload gsmaApiPayload;

    @Autowired
    MojaloopApiPayload mojaloopApiPayload;

    @Autowired
    PaymentModeConfiguration paymentModeConfiguration;

    @PostConstruct
    private void registerApiProcessor() {
        for (PaymentModeMapping paymentMode : paymentModeConfiguration.getMappings()) {
            if (paymentMode.getId().equalsIgnoreCase("gsma")) {
                payloadMap.put(paymentMode.getId(), gsmaApiPayload);
            } else if (paymentMode.getId().equalsIgnoreCase("mojaloop")) {
                payloadMap.put(paymentMode.getId(), mojaloopApiPayload);
            }
        }
    }

    public Function<Exchange, String> getApiPayloadSetter(String paymentMode) {
        PaymentModeMapping mapping = paymentModeConfiguration.getByMode(paymentMode);
        return payloadMap.get(mapping.getId());
    }

}
