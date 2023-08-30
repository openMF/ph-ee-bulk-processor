package org.mifos.processor.bulk.format.helper;

import org.mifos.processor.bulk.schema.CsvSchema;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseMapper<FROM extends CsvSchema, TO extends CsvSchema> implements Mapper<FROM, TO> {

    @Override
    public List<TO> convertList(List<FROM> objects) {
        List<TO> list = new ArrayList<>();
        objects.forEach(transaction -> {
            list.add(convert(transaction));
        });
        return list;
    }
}
