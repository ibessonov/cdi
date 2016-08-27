package org.ibess.cdi.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ibessonov
 */
public final class TempStorage {
    public static final Map<String, Object> MAP = new ConcurrentHashMap<>();
}
