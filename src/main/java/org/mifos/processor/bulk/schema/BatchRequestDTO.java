package org.mifos.processor.bulk.schema;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BatchRequestDTO {

    String requestId;
    List<Party> creditParty;
    List<Party> debitParty;

    String paymentMode;
    String amount;
    String currency;
    String descriptionText;

}
