package org.mifos.processor.bulk.schema;

import java.util.Arrays;

public enum PartyType {

    MSISDN("msisdn"), ACCOUNT_NUMBER("accountnumber"), EMPTY("");

    private String value;

    PartyType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static PartyType fromValue(String value) {
        return Arrays.stream(values()).filter(ec -> ec.getValue().equalsIgnoreCase(value)).findFirst().orElseGet(() -> PartyType.EMPTY);
    }
}
