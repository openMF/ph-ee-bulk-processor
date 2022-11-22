package org.mifos.processor.bulk.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "bpmn")
@Setter
@Getter
public class PaymentModeConfiguration {

    private List<PaymentModeApiMapping> mappings = new ArrayList<>();

    public PaymentModeApiMapping getByMode(String paymentMode) {
        return getMappings().stream()
                .filter(p -> p.getId().equalsIgnoreCase(paymentMode))
                .findFirst()
                .orElse(null);
    }

}
