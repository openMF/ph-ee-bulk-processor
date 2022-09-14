package org.mifos.processor.bulk.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// id,request_id,payment_mode,payer_identifier_type,
// payer_identifier,payee_identifier_type,payee_identifier,
// amount,currency,note

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "id", "request_id", "payment_mode", "payer_identifier_type", "payer_identifier", "payee_identifier_type", "payee_identifier", "currency","amount", "note", "program_shortcode",  })
public class TestSchema {

    @JsonProperty("id")
    private int id;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("payment_mode")
    private String paymentMode;

    @JsonProperty("payer_identifier_type")
    private String payerIdentifierType;

    @JsonProperty("payer_identifier")
    private String payerIdentifier;

    @JsonProperty("payee_identifier_type")
    private String payeeIdentifierType;

    @JsonProperty("payee_identifier")
    private String payeeIdentifier;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("note")
    private String note;

    @JsonProperty("program_shortcode")
    private String programShortCode;

    @JsonProperty("cycle")
    private String cycle;

    public TestSchema() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getPayerIdentifierType() {
        return payerIdentifierType;
    }

    public void setPayerIdentifierType(String payerIdentifierType) {
        this.payerIdentifierType = payerIdentifierType;
    }

    public String getPayerIdentifier() {
        return payerIdentifier;
    }

    public void setPayerIdentifier(String payerIdentifier) {
        this.payerIdentifier = payerIdentifier;
    }

    public String getPayeeIdentifierType() {
        return payeeIdentifierType;
    }

    public void setPayeeIdentifierType(String payeeIdentifierType) {
        this.payeeIdentifierType = payeeIdentifierType;
    }

    public String getPayeeIdentifier() {
        return payeeIdentifier;
    }

    public void setPayeeIdentifier(String payeeIdentifier) {
        this.payeeIdentifier = payeeIdentifier;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getProgramShortCode() {
        return programShortCode;
    }

    public void setProgramShortCode(String programShortCode) {
        this.programShortCode = programShortCode;
    }

    public String getCycle() {
        return cycle;
    }

    public void setCycle(String cycle) {
        this.cycle = cycle;
    }
}
