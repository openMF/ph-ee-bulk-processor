package org.mifos.processor.bulk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class SubBatchEntity {

    private String batchId, subBatchId, requestId, requestFile, resultFile, note, paymentMode,
            registeringInstitutionId, payerFsp, correlationId;

    private Long totalTransactions, ongoing, failed, completed, totalAmount, ongoingAmount,
            failedAmount, completedAmount, workflowKey, workflowInstanceKey, approvedAmount, approvedCount;

    private Date resultGeneratedAt, startedAt, completedAt;

    @JsonIgnore
    public void setAllEmptyAmount() {
        setTotalTransactions(0L);
        setOngoing(0L);
        setFailed(0L);
        setCompleted(0L);
        setTotalAmount(0L);
        setOngoingAmount(0L);
        setFailedAmount(0L);
        setCompletedAmount(0L);
        setApprovedAmount(0L);
        setApprovedCount(0L);
    }
}
