package org.mifos.processor.bulk.format;

import java.util.List;

public interface EntityMapper<T, K> {

    T convertTo(K object);

    K convertFrom(T object);

    List<T> convertListTo(List<K> objects);

    List<K> convertListFrom(List<T> objects);
}
