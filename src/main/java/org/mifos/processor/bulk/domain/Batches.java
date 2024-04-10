package org.mifos.processor.bulk.domain;

import java.sql.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Batches {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "BATCH_ID")
    private String batchId;
    @Column(name = "REQUEST_ID")
    private String requestId;

    @Column(name = "REQUEST_FILE")
    private String requestFile;

    @Column(name = "TOTAL_TRANSACTIONS")
    private Long totalTransactions;

    @Column(name = "ONGOING")
    private Long ongoing;

    @Column(name = "FAILED")
    private Long failed;

    @Column(name = "COMPLETED")
    private Long completed;

    @Column(name = "TOTAL_AMOUNT")
    private Long totalAmount;

    @Column(name = "ONGOING_AMOUNT")
    private Long ongoingAmount;

    @Column(name = "FAILED_AMOUNT")
    private Long failedAmount;

    @Column(name = "COMPLETED_AMOUNT")
    private Long completedAmount;

    @Column(name = "RESULT_FILE")
    private String resultFile;

    @Column(name = "RESULT_GENERATED_AT")
    private Date resultGeneratedAt;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "WORKFLOW_KEY")
    private Long workflowKey;

    @Column(name = "WORKFLOW_INSTANCE_KEY")
    private Long workflowInstanceKey;

    @Column(name = "STARTED_AT")
    private Date startedAt;

    @Column(name = "COMPLETED_AT")
    private Date completedAt;

    @Column(name = "PAYMENT_MODE")
    private String paymentMode;

    @Column(name = "REGISTERING_INSTITUTION_ID")
    private String registeringInstitutionId;

    @Column(name = "PAYER_FSP")
    private String payerFsp;

    @Column(name = "CLIENT_CORRELATION_ID")
    private String correlationId;

    @Column(name = "APPROVED_AMOUNT")
    private Long approvedAmount;

    @Column(name = "APPROVED_COUNT")
    private Long approvedCount;

    public Batches(Long id, String batchId, String requestId, String requestFile, Long totalTransactions, Long ongoing, Long failed,
            Long completed, Long totalAmount, Long ongoingAmount, Long failedAmount, Long completedAmount, String resultFile,
            Date resultGeneratedAt, String note, Long workflowKey, Long workflowInstanceKey, Date startedAt, Date completedAt,
            String paymentMode, String registeringInstitutionId, String payerFsp, String correlationId, Long approvedAmount,
            Long approvedCount) {
        this.id = id;
        this.batchId = batchId;
        this.requestId = requestId;
        this.requestFile = requestFile;
        this.totalTransactions = totalTransactions;
        this.ongoing = ongoing;
        this.failed = failed;
        this.completed = completed;
        this.totalAmount = totalAmount;
        this.ongoingAmount = ongoingAmount;
        this.failedAmount = failedAmount;
        this.completedAmount = completedAmount;
        this.resultFile = resultFile;
        this.resultGeneratedAt = resultGeneratedAt;
        this.note = note;
        this.workflowKey = workflowKey;
        this.workflowInstanceKey = workflowInstanceKey;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.paymentMode = paymentMode;
        this.registeringInstitutionId = registeringInstitutionId;
        this.payerFsp = payerFsp;
        this.correlationId = correlationId;
        this.approvedAmount = approvedAmount;
        this.approvedCount = approvedCount;
    }

    public Batches() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestFile() {
        return requestFile;
    }

    public void setRequestFile(String requestFile) {
        this.requestFile = requestFile;
    }

    public Long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(Long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public Long getOngoing() {
        return ongoing;
    }

    public void setOngoing(Long ongoing) {
        this.ongoing = ongoing;
    }

    public Long getFailed() {
        return failed;
    }

    public void setFailed(Long failed) {
        this.failed = failed;
    }

    public Long getCompleted() {
        return completed;
    }

    public void setCompleted(Long completed) {
        this.completed = completed;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getOngoingAmount() {
        return ongoingAmount;
    }

    public void setOngoingAmount(Long ongoingAmount) {
        this.ongoingAmount = ongoingAmount;
    }

    public Long getFailedAmount() {
        return failedAmount;
    }

    public void setFailedAmount(Long failedAmount) {
        this.failedAmount = failedAmount;
    }

    public Long getCompletedAmount() {
        return completedAmount;
    }

    public void setCompletedAmount(Long completedAmount) {
        this.completedAmount = completedAmount;
    }

    public String getResultFile() {
        return resultFile;
    }

    public void setResultFile(String resultFile) {
        this.resultFile = resultFile;
    }

    public Date getResultGeneratedAt() {
        return resultGeneratedAt;
    }

    public void setResultGeneratedAt(Date resultGeneratedAt) {
        this.resultGeneratedAt = resultGeneratedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getWorkflowKey() {
        return workflowKey;
    }

    public void setWorkflowKey(Long workflowKey) {
        this.workflowKey = workflowKey;
    }

    public Long getWorkflowInstanceKey() {
        return workflowInstanceKey;
    }

    public void setWorkflowInstanceKey(Long workflowInstanceKey) {
        this.workflowInstanceKey = workflowInstanceKey;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getRegisteringInstitutionId() {
        return registeringInstitutionId;
    }

    public void setRegisteringInstitutionId(String registeringInstitutionId) {
        this.registeringInstitutionId = registeringInstitutionId;
    }

    public String getPayerFsp() {
        return payerFsp;
    }

    public void setPayerFsp(String payerFsp) {
        this.payerFsp = payerFsp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Long getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(Long approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public Long getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(Long approvedCount) {
        this.approvedCount = approvedCount;
    }
}
