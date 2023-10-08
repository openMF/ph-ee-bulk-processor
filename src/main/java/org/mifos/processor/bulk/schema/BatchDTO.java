package org.mifos.processor.bulk.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BatchDTO {

    private String batch_id;

    private String request_id;

    private Long total;

    private Long ongoing;

    private Long failed;

    private Long successful;

    private BigDecimal totalAmount;

    private BigDecimal successfulAmount;

    private BigDecimal pendingAmount;

    private BigDecimal failedAmount;

    private String file;

    private String notes;

    private String created_at;

    private String status;

    private String modes;

    private String purpose;

    private String failPercentage;

    private String successPercentage;

    private String registeringInstitutionId;

    private String payerFsp;

    private String correlationId;

}
