package org.mifos.processor.bulk.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountLookupResponseDTO implements Serializable {
    private String requestId;
    private String payeeIdentity;
    private List<PaymentModalityDTO> paymentModalityList;

}
