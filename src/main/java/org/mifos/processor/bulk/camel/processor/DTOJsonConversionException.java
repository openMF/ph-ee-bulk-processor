package org.mifos.processor.bulk.camel.processor;

public class DTOJsonConversionException extends RuntimeException {

    public <T> DTOJsonConversionException(Class<T> dtoClass, String message, Throwable cause) {
        super(message, cause);
    }
}
