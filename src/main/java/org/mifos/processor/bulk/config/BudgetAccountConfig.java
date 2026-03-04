package org.mifos.processor.bulk.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "budget-account")
public class BudgetAccountConfig {

    private List<RegisteringInstitutionConfig> registeringInstitutions = new ArrayList<>();

    public RegisteringInstitutionConfig getByRegisteringInstituteId(String id) {
        return getRegisteringInstitutions().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }
}
