package org.ibess.cdi.util;

import java.util.Collection;

/**
 * @author ibessonov
 */
public class CollectionUtil {

    @SafeVarargs
    public static <T> T[] array(T... t) {
        return t;
    }

    public static <T> void addIfNotNull(Collection<? super T> collection, T element) {
        if (element != null) {
            collection.add(element);
        }
    }
}
