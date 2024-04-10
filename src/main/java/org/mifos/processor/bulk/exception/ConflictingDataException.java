package org.mifos.processor.bulk.exception;

public class ConflictingDataException extends RuntimeException {

    private final String conflictingFieldName;
    private final String conflictingFieldValue;

    public ConflictingDataException(String fieldName, String fieldValue) {
        super(String.format("Conflicting data detected for field: %s, value: %s", fieldName, fieldValue));
        this.conflictingFieldName = fieldName;
        this.conflictingFieldValue = fieldValue;
    }

    public String getConflictingFieldName() {
        return conflictingFieldName;
    }

    public String getConflictingFieldValue() {
        return conflictingFieldValue;
    }

}
