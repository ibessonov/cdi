package org.ibess.cdi;

import org.ibess.cdi.annotations.Constructor;
import org.ibess.cdi.annotations.Inject;
import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.exceptions.CdiException;
import org.ibess.cdi.internal.$CdiObject;
import org.ibess.cdi.internal.$Context;
import org.ibess.cdi.util.Lazy;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;

import java.io.Serializable;
import java.util.List;

import static org.ibess.cdi.enums.Scope.SINGLETON;
import static org.ibess.cdi.internal.$Descriptor.$;
import static org.ibess.cdi.internal.$Descriptor.$0;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

/**
 * @author ibessonov
 */
@SuppressWarnings("unchecked")
@FixMethodOrder(NAME_ASCENDING)
public class ContextTest {

    private $Context context;

    @Before
    public void setUp() {
        context = new ContextImpl();
    }

    @Scoped(SINGLETON) static class Singleton {
        @Inject Singleton instance;
        @Inject GenericClass<String> generic;
    }

    @Scoped static class GenericClass<T> {
        @Inject GenericInnerClass<T> value;
    }

    @Scoped static class GenericInnerClass<T> {
        @Inject Class<T> c;
    }

    @Test
    public void lookup() {
        Singleton singleton = context.lookup(Singleton.class);

        assertEquals(singleton, singleton.instance);

        assertNotEquals(GenericClass.class, singleton.generic.getClass());
        assertTrue(singleton.generic instanceof $CdiObject);

        assertNotEquals(GenericInnerClass.class, singleton.generic.value.getClass());
        assertTrue(singleton.generic.value instanceof $CdiObject);
    }

    @Scoped static class WithConstructor {
        int value;
        @Constructor public void constructor(Singleton param) {
            value = 15;
        }
    }

    @Test
    public void constructor() {
        WithConstructor withConstructor = context.lookup(WithConstructor.class);

        assertEquals(15, withConstructor.value);
    }

    @Scoped static class WithContext {
        @Inject Context context;
    }

    @Test
    public void context() {
        WithContext withContext = context.lookup(WithContext.class);

        assertSame(context, withContext.context);
    }

    @Scoped static class GenericPair<T, V> {
        @Inject GenericInnerClass<V> v;
        @Inject GenericInnerClass<T> t;
    }

    @Test
    public void genericPair() {
        GenericPair pair = (GenericPair) context.$lookup($(GenericPair.class, $0(Integer.class), $0(Double.class)));

        assertEquals(Integer.class, pair.t.c);
        assertEquals(Double.class, pair.v.c);
    }

    @Scoped static class Erased<T> {
        @Inject GenericInnerClass value;
    }

    @Test(expected = CdiException.class)
    public void erased() {
        context.$lookup($(Erased.class, $0(String.class)));
    }

    @Scoped static class Wildcard<T> {
        @Inject GenericInnerClass<?> value;
    }

    @Test(expected = CdiException.class)
    public void wildcard() {
        context.$lookup($(Wildcard.class, $0(String.class)));
    }

    @Scoped static class InjectedClass<T extends CharSequence & Serializable, V extends List<? super T>> {
        @Inject Class<T> clazz;
    }

    @Test
    public void injectedClass() {
        InjectedClass injectedClass = (InjectedClass) context.$lookup($(InjectedClass.class, $0(String.class), $0(List.class)));
        assertEquals(String.class, injectedClass.clazz);
    }

    @Scoped static class Shitty<T> {
        @Inject Class<T> clazz;
        @Inject T value;
    }

    @Scoped static class Holder {
        @Inject Shitty<Shitty<String>> shitty;
    }

    @Test
    public void shitty() {
        Holder holder = context.lookup(Holder.class);
        assertSame(String.class, holder.shitty.value.clazz);
    }

    @Scoped static class WithLazy<T> {
        @Inject Lazy<T> lazy;
    }

    @Test
    public void lazy() {
        WithLazy<Singleton> lazy = (WithLazy) context.$lookup($(WithLazy.class, $0(Singleton.class)));
        assertSame(lazy.lazy.get(), lazy.lazy.get().instance);
    }

    @Test(timeout = 100)
    public void zLookupPerformance() {
        for (int i = 0; i < 10000; i++) {
            context.$lookup($(GenericPair.class, $0(String.class), $0(List.class)));
        }
    }
}