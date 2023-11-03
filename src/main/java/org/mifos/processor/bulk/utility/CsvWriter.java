package org.mifos.processor.bulk.utility;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.io.IOException;
import java.util.List;

public final class CsvWriter {

    private CsvWriter() {}

    public static <T> void writeToCsv(List<T> data, Class<T> tClass, CsvMapper csvMapper, boolean overrideHeader, String filepath)
            throws IOException {
        CsvSchema csvSchema = csvMapper.schemaFor(tClass);
        if (overrideHeader) {
            csvSchema = csvSchema.withHeader();
        } else {
            csvSchema = csvSchema.withoutHeader();
        }

        File file = new File(filepath);
        if (!file.exists()) {
            file.createNewFile();
        }
        SequenceWriter writer = csvMapper.writerWithSchemaFor(tClass).with(csvSchema).writeValues(file);
        for (T object : data) {
            writer.write(object);
        }
    }

}
