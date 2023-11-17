package org.mifos.processor.bulk.schema;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchAccountLookupResponseDTO {

    private String requestId;
    private String registeringInstitutionId;
    private List<BeneficiaryDTO> beneficiaryDTOList;
}
