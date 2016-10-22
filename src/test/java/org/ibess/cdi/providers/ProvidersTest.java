package org.ibess.cdi.providers;

import org.ibess.cdi.CdiTest;
import org.ibess.cdi.Extension;
import org.ibess.cdi.annotations.Inject;
import org.ibess.cdi.annotations.Scoped;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.ibess.cdi.util.CollectionUtil.array;
import static org.junit.Assert.assertSame;

/**
 * @author ibessonov
 */
public class ProvidersTest extends CdiTest {

    interface MyInterface {}

    private static final MyInterface INSTANCE = new MyInterface() {};

    @Override
    public Extension[] getExtensions() {
        return array(r -> r.registerProvider(MyInterface.class, () -> INSTANCE));
    }

    @Scoped static class WithCustomProvidedField {
        @Inject MyInterface value;
    }

    @Test
    public void customProvider() {
        WithCustomProvidedField withCustomProvidedField = context.lookup(WithCustomProvidedField.class);
        assertSame(INSTANCE, withCustomProvidedField.value);
    }

    @Scoped static class WithProvidedFields {
        @Inject List list;
        @Inject Map map;
        @Inject Set set;
        @Inject SortedMap sortedMap;
        @Inject SortedSet sortedSet;
        @Inject ConcurrentMap concurrentMap;
    }

    @Test
    public void defaultProviders() {
        WithProvidedFields withProvidedFields = context.lookup(WithProvidedFields.class);
        assertEqualsWithType(new ArrayList<>(), withProvidedFields.list);
        assertEqualsWithType(new HashMap<>(), withProvidedFields.map);
        assertEqualsWithType(new HashSet<>(), withProvidedFields.set);
        assertEqualsWithType(new TreeMap<>(), withProvidedFields.sortedMap);
        assertEqualsWithType(new TreeSet<>(), withProvidedFields.sortedSet);
        assertEqualsWithType(new ConcurrentHashMap<>(), withProvidedFields.concurrentMap);
    }
}
