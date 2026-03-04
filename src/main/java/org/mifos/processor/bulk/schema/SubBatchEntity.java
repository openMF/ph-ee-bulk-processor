package org.mifos.processor.bulk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubBatchEntity {

    private String batchId;
    private String subBatchId;
    private String requestId;
    private String requestFile;
    private String resultFile;
    private String note;
    private String paymentMode;
    private String registeringInstitutionId;
    private String payerFsp;
    private String correlationId;

    private Long totalTransactions;
    private Long ongoing;
    private Long failed;
    private Long completed;
    private Long totalAmount;
    private Long ongoingAmount;
    private Long failedAmount;
    private Long completedAmount;
    private Long workflowKey;
    private Long workflowInstanceKey;
    private Long approvedAmount;
    private Long approvedCount;

    private Date resultGeneratedAt;
    private Date startedAt;
    private Date completedAt;

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
