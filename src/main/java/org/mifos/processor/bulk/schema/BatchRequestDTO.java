package org.mifos.processor.bulk.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BatchRequestDTO {

    List<Party> creditParty, debitParty;

    String paymentMode, amount, currency, descriptionText;

}
