package org.ibess.cdi.util;

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * @author ibessonov
 */
public class CollectionUtil {

    @SafeVarargs
    public static <T> T[] array(T... t) {
        return t;
    }

    public static <T> T[] join(T[] l, T[] r) {
        if (r.length == 0) return l;
        if (l.length == 0) return r;

        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(l.getClass().getComponentType(), l.length + r.length);
        System.arraycopy(l, 0, result, 0, l.length);
        System.arraycopy(r, 0, result, l.length, r.length);
        return result;
    }

    public static <T> void addIfNotNull(Collection<? super T> collection, T element) {
        if (element != null) {
            collection.add(element);
        }
    }
}
