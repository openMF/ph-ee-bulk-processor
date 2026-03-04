package org.mifos.processor.bulk.format.helper;

import java.util.ArrayList;
import java.util.List;
import org.mifos.processor.bulk.schema.CsvSchema;

public abstract class BaseMapper<F extends CsvSchema, T extends CsvSchema> implements Mapper<F, T> {

    @Override
    public List<T> convertList(List<F> objects) {
        List<T> list = new ArrayList<>();
        objects.forEach(transaction -> {
            list.add(convert(transaction));
        });
        return list;
    }
}
