package org.mifos.processor.bulk.schema;

public class AuthorizationRequest {

    private String batchId;

    private String payerIdentifier;

    private String currency;

    private String amount;

    public AuthorizationRequest(){}

    public AuthorizationRequest(String batchId, String payerIdentifier, String currency, String amount){
        this.batchId = batchId;
        this.payerIdentifier = payerIdentifier;
        this.currency = currency;
        this.amount = amount;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getPayerIdentifier() {
        return payerIdentifier;
    }

    public void setPayerIdentifier(String payerIdentifier) {
        this.payerIdentifier = payerIdentifier;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
