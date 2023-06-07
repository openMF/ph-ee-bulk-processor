package org.mifos.processor.bulk.camel.processor;

public class DTOJsonConversionException extends RuntimeException {

    public <T> DTOJsonConversionException(Class<T> dtoClass) {
        super("Unable to convert " + dtoClass.getName() + "to json string");
    }
}
