package org.ibess.cdi.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ibessonov
 */
public final class RuntimeUtil {

    public static final Map<String, Object> TEMP_STORAGE = new ConcurrentHashMap<>();

    public static void noop() {}
}
