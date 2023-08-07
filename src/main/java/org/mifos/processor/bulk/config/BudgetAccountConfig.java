package org.mifos.processor.bulk.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "budget-account")
public class BudgetAccountConfig {

    private List<RegisteringInstitutionConfig> registeringInstitutions = new ArrayList<>();

    public RegisteringInstitutionConfig getByRegisteringInstituteId(String id) {
        return getRegisteringInstitutions().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
