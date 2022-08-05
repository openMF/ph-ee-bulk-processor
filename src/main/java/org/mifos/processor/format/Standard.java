package org.mifos.processor.format;

public enum Standard {

    GSMA(StandardValue.GSMA),
    DEFAULT(StandardValue.DEFAULT);

    private final String value;

    Standard(String s) {
        value = s;
    }

    public String getValue() {
        return value;
    }

}

