package org.mifos.processor.bulk.schema;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "id", "requestId", "paymentMode", "accountNumber", "amount", "note" })
public class GSMATransaction implements CsvSchema {

    private int id;
    private String requestId;
    private String paymentMode;
    private String accountNumber;
    private String amount;

    private String note;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @Override
    public String getCsvString() {
        return String.format("%s,%s,%s,%s,%s,%s", id, requestId, paymentMode, accountNumber, amount, note);
    }

    @Override
    public String getCsvHeader() {
        return "id,requestId,paymentMode,accountNumber,amount,note";
    }
}
