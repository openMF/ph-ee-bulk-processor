package org.mifos.processor.bulk.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConflictResponseDTO {

    private String responseCode;
    private String responseDescription;
    private String conflictingFieldName;
    private String conflictingFieldValue;
}
