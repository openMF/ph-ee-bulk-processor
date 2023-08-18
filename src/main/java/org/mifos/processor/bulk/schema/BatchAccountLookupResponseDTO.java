package org.mifos.processor.bulk.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchAccountLookupResponseDTO {
    private String requestId;
    private String registeringInstitutionId;
    private List<BeneficiaryDTO> beneficiaryDTOList;
}
