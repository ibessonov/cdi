package org.ibess.cdi.util;

import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.enums.Scope;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author ibessonov
 */
public class ScopedAnnotationCache {

    private static final Map<Class<?>, Scope> cache = new WeakHashMap<>();

    public static synchronized Scope getScope(Class<?> clazz) {
        if (cache.containsKey(clazz)) {
            return cache.get(clazz);
        }
        Scoped scoped = clazz.getAnnotation(Scoped.class);
        if (scoped == null) {
            cache.put(clazz, null);
            return null;
        } else {
            Scope scope = scoped.value();
            cache.put(clazz, scope);
            return scope;
        }
    }
}
