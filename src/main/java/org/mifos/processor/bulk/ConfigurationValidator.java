package org.mifos.processor.bulk;

import org.mifos.processor.bulk.schema.Transaction;
import org.mifos.processor.bulk.format.Standard;
import org.mifos.processor.bulk.zeebe.worker.WorkerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConfigurationValidator {

    public Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${config.ordering.field}")
    private String orderingField;

    @Value("${config.success-threshold-check.success-threshold}")
    private int successRate;

    @Value("${config.success-threshold-check.max-retry}")
    private int maxThresholdCheckRetry;

    @Value("${config.formatting.standard}")
    private String standard;

    @Autowired
    private WorkerConfig workerConfig;

    @PostConstruct
    private void validate() {
        if (workerConfig.isOrderingWorkerEnabled) {
            validateOrderingConfig();
        }
        if (workerConfig.isSuccessThresholdCheckEnabled) {
            validateSuccessThresholdConfig();
            validateMaxRetryFromThresholdCheck();
        }
        if (workerConfig.isFormattingWorkerEnabled) {
            validateFormattingStandard();
        }
    }

    private void validateMaxRetryFromThresholdCheck() {
        if (maxThresholdCheckRetry <= 0) {
            logger.error("Invalid maxThresholdCheckRetry count set. Needs to be +ve integer");
            throw new ConfigurationValidationException("Invalid maxThresholdCheckRetry count set. Needs to be +ve integer");
        }
    }

    // validates the standard to be used for formatting
    private void validateFormattingStandard() {
        String std = this.standard.toUpperCase();
        try {
            Standard standardEnum = Standard.valueOf(std);
            logger.info("Configured formatting standard as >> {}", standardEnum.name());
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> possibleStandards = new ArrayList<>();
        for (Field f: Standard.class.getFields()) {
            possibleStandards.add(f.getName());
        }
        throw new ConfigurationValidationException("Invalid standard configured for formatting data. Possible values are ["
        + String.join(",", possibleStandards) + "]");
    }

    // validates the ordering configuration
    private void validateOrderingConfig() {
        List<String> possibleOrderingFields = new ArrayList<>();

        for (Field field: Transaction.class.getDeclaredFields()) {
            possibleOrderingFields.add(field.getName());
        }

        if (!possibleOrderingFields.contains(orderingField)) {
            throw new ConfigurationValidationException("Invalid ordering field, possible values are ["
                    + String.join(",", possibleOrderingFields) + "]");
        }
    }

    // validates the success threshold related configuration
    private void validateSuccessThresholdConfig() {
        if (successRate <= 0 || successRate > 100) {
            throw new ConfigurationValidationException("Invalid success threshold value configured (value=" + successRate + ").");
        }

        if (successRate < 50) {
            logger.warn("It is advised to set the success threshold greater than 50. Currently configured as {}", successRate);
        }
    }

    // this exception is thrown when unexpected application config is set, and can't pass the ConfigurationValidator
    public static class ConfigurationValidationException extends RuntimeException {
        ConfigurationValidationException(String message) { super(message); }
    }
}
