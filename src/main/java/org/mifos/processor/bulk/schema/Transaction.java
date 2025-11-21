package org.mifos.processor.bulk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "id", "request_id", "payment_mode", "payer_identifier_type", "payer_identifier", "payee_identifier_type",
        "payee_identifier", "amount", "currency", "note", "program_shortcode", "cycle", "payee_dfsp_id", "batch_id", "account_number" })
public class Transaction implements CsvSchema {

    @JsonProperty("id")
    private int id;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("payment_mode")
    private String paymentMode;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("currency")
    private String currency;

    @Override
    public boolean equals(Object transaction) {
        if (this == transaction) {
            return true;
        }
        if ((transaction == null) || (getClass() != transaction.getClass())) {
            return false;
        }
        Transaction that = (Transaction) transaction;
        return (id == that.id) && (Objects.equals(requestId, that.requestId)) && (Objects.equals(paymentMode, that.paymentMode))
                && (Objects.equals(accountNumber, that.accountNumber)) && (Objects.equals(amount, that.amount))
                && (Objects.equals(payeeDfspId, that.payeeDfspId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, requestId, paymentMode, accountNumber, amount, currency, note, payerIdentifierType, payerIdentifier,
                payeeIdentifierType, payeeIdentifier, payeeDfspId);
    }

    @JsonProperty("note")
    private String note;

    @JsonProperty(value = "payer_identifier_type")
    private String payerIdentifierType;

    @JsonProperty("payer_identifier")
    private String payerIdentifier;

    @JsonProperty("payee_identifier_type")
    private String payeeIdentifierType;

    @JsonProperty("payee_identifier")
    private String payeeIdentifier;

    @JsonProperty("program_shortcode")
    private String programShortCode;

    @JsonProperty("cycle")
    private String cycle;

    @JsonProperty("payee_dfsp_id")
    private String payeeDfspId;

    @JsonProperty("batch_id")
    private String batchId;

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("Transaction{");
        buffer.append("id=").append(id);
        buffer.append(", request_id='").append(requestId);
        buffer.append(", payment_mode='").append(paymentMode);
        buffer.append(", account_number='").append(accountNumber);
        buffer.append(", amount='").append(amount);
        buffer.append(", currency='").append(currency);
        buffer.append(", note='").append(note);
        buffer.append(", batchId='").append(batchId);
        buffer.append(", status='").append(id).append('}');
        return buffer.toString();
    }

    @JsonIgnore
    @Override
    public String getCsvString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s", id, requestId, paymentMode, accountNumber, amount, currency, note);
    }

    @JsonIgnore
    @Override
    public String getCsvHeader() {
        return "id,request_id,payment_mode,account_number,amount,currency,note,status";
    }
}
