package org.mifos.processor.bulk.format.helper;

import java.util.List;
import org.mifos.processor.bulk.schema.CsvSchema;

public interface Mapper<FROM extends CsvSchema, TO extends CsvSchema> {

    /**
     * Use for converting the object of type [FROM] to an object of type [TO]
     *
     * @param object
     *            of type [FROM]
     * @return object of type [To]
     */
    public TO convert(FROM object);

    /**
     * Use for converting the list of objects of type [FROM] to an object of type [TO]
     *
     * @param objects
     *            lost of object of type [FROM]
     * @return objects of type [To]
     */
    public List<TO> convertList(List<FROM> objects);

}
