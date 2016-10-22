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

        T[] result = newArray(l, l.length + r.length);
        System.arraycopy(l, 0, result, 0, l.length);
        System.arraycopy(r, 0, result, l.length, r.length);
        return result;
    }

    public static <T> T[] drop(int count, T[] t) {
        if (count == 0) return t;

        T[] result = newArray(t, t.length - count);
        System.arraycopy(t, count, result, 0, result.length);
        return result;
    }

    public static <T> void addIfNotNull(Collection<? super T> collection, T element) {
        if (element != null) {
            collection.add(element);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] newArray(T[] old, int size) {
        Class<?> clazz = old.getClass();
        if (clazz == Object[].class) {
            return (T[]) new Object[size];
        } else {
            return (T[]) Array.newInstance(clazz.getComponentType(), size);
        }
    }
}
