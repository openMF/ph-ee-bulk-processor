package org.mifos.processor.bulk.format.helper;

import java.util.ArrayList;
import java.util.List;
import org.mifos.processor.bulk.schema.CsvSchema;

public abstract class BaseMapper<From extends CsvSchema, To extends CsvSchema> implements Mapper<From, To> {

    @Override
    public List<To> convertList(List<From> objects) {
        List<To> list = new ArrayList<>();
        objects.forEach(transaction -> {
            list.add(convert(transaction));
        });
        return list;
    }
}
