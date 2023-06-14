package org.mifos.processor.bulk.format;

public enum Standard {

    GSMA(StandardValue.GSMA), DEFAULT(StandardValue.DEFAULT);

    private final String value;

    Standard(String s) {
        value = s;
    }

    public String getValue() {
        return value;
    }

}
