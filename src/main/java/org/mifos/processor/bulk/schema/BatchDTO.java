package org.mifos.processor.bulk.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class BatchDTO {

    private String batchId;

    private String requestId;

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

    private String createdAt;

    private String status;

    private String modes;

    private String purpose;

    private String failPercentage;

    private String successPercentage;

    private String registeringInstitutionId;

    private String payerFsp;

    private String correlationId;

    public BatchDTO() {}

    public BatchDTO(String batchId, String requestId, Long totalTransactions, Long ongoing, Long failed, Long completed,
            BigDecimal total_amount, BigDecimal completed_amount, BigDecimal ongoing_amount, BigDecimal failed_amount, String result_file,
            String note) {
        this.batchId = batchId;
        this.requestId = requestId;
        this.total = totalTransactions;
        this.ongoing = ongoing;
        this.failed = failed;
        this.successful = completed;
        this.totalAmount = total_amount;
        this.successfulAmount = completed_amount;
        this.pendingAmount = ongoing_amount;
        this.failedAmount = failed_amount;
        this.file = result_file;
        this.notes = note;
    }

    public BatchDTO(String batch_id, String request_id, Long total, Long ongoing, Long failed, Long successful, BigDecimal totalAmount,
            BigDecimal successfulAmount, BigDecimal pendingAmount, BigDecimal failedAmount, String file, String notes, String created_at,
            String status, String modes, String purpose) {
        this.batchId = batch_id;
        this.requestId = request_id;
        this.total = total;
        this.ongoing = ongoing;
        this.failed = failed;
        this.successful = successful;
        this.totalAmount = totalAmount;
        this.successfulAmount = successfulAmount;
        this.pendingAmount = pendingAmount;
        this.failedAmount = failedAmount;
        this.file = file;
        this.notes = notes;
        this.createdAt = created_at;
        this.status = status;
        this.modes = modes;
        this.purpose = purpose;
    }
}
