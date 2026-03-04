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
@ConfigurationProperties(prefix = "budget-account.registering-institutions")
public class RegisteringInstitutionConfig {

    private String id;
    private List<Program> programs = new ArrayList<>();

    public Program getByProgramId(String id) {
        return getPrograms().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

}
