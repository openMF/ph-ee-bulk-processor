package org.mifos.processor.bulk.schema;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "id", "request_id", "payment_mode", "account_number", "amount", "currency", "note" })
public class Transaction implements CsvSchema {

    private int id;
    private String request_id;
    private String payment_mode;
    private String account_number;
    private String amount;
    private String currency;
    private String note;
    private String batchId;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getPayment_mode() {
        return payment_mode;
    }

    public void setPayment_mode(String payment_mode) {
        this.payment_mode = payment_mode;
    }

    public String getAccount_number() {
        return account_number;
    }

    public void setAccount_number(String account_number) {
        this.account_number = account_number;
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

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", request_id='" + request_id + '\'' +
                ", payment_mode='" + payment_mode + '\'' +
                ", account_number='" + account_number + '\'' +
                ", amount='" + amount + '\'' +
                ", currency='" + currency + '\'' +
                ", note='" + note + '\'' +
                ", batchId='" + batchId + '\'' +
                '}';
    }

    @Override
    public String getCsvString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s", id, request_id, payment_mode, account_number, amount, currency, note);
    }

    @Override
    public String getCsvHeader() {
        return "id,request_id,payment_mode,account_number,amount,currency,note";
    }

}
