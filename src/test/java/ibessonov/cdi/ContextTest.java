package ibessonov.cdi;

import ibessonov.cdi.annotations.Constructor;
import ibessonov.cdi.annotations.Inject;
import ibessonov.cdi.annotations.Scoped;
import ibessonov.cdi.internal.$CdiObject;
import ibessonov.cdi.util.Lazy;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.List;

import static ibessonov.cdi.enums.Scope.SINGLETON;
import static ibessonov.cdi.util.Cdi.getTypeParameters;
import static org.junit.Assert.*;

/**
 * @author ibessonov
 */
public class ContextTest {

    private ContextImpl context;

    @Before
    public void setUp() {
        context = new ContextImpl();
    }

    @Scoped(SINGLETON)
    static class Singleton {
        @Inject Singleton instance;
        @Inject GenericClass<String> generic;
    }

    @Scoped static class GenericClass<T> {
        @Inject GenericInnerClass<T> value;
    }

    @Scoped static class GenericInnerClass<T> {
    }

    @Test
    public void lookup() {
        Singleton singleton = context.lookup(Singleton.class);

        assertEquals(singleton, singleton.instance);

        assertNotEquals(GenericClass.class, singleton.generic.getClass());
        assertTrue(singleton.generic instanceof $CdiObject);
        assertArrayEquals(new Object[] { String.class }, getTypeParameters(singleton.generic));

        assertNotEquals(GenericInnerClass.class, singleton.generic.value.getClass());
        assertTrue(singleton.generic.value instanceof $CdiObject);
        assertArrayEquals(new Object[] { String.class }, getTypeParameters(singleton.generic.value));
    }

    @Scoped static class WithConstructor {
        public int value;
        @Constructor public void constructor() {
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
        GenericPair pair = context.lookup(GenericPair.class, Integer.class, Double.class);

        assertArrayEquals(new Object[] { Integer.class }, getTypeParameters(pair.t));
        assertArrayEquals(new Object[] { Double.class  }, getTypeParameters(pair.v));
    }

    @Scoped static class Erased<T> {
        @Inject GenericInnerClass value;
    }

    @Test(expected = RuntimeException.class)
    public void erased() {
        context.lookup(Erased.class, String.class);
    }

    @Scoped static class Wildcard<T> {
        @Inject GenericInnerClass<?> value;
    }

    @Test(expected = RuntimeException.class)
    public void wildcard() {
        context.lookup(Wildcard.class, String.class);
    }

    @Scoped static class InjectedClass<T extends CharSequence & Serializable, V extends List<? super T>> {
        @Inject Class<T> clazz;
    }

    @Test
    public void injectedClass() {
        InjectedClass injectedClass = context.lookup(InjectedClass.class, String.class, List.class);
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
    @SuppressWarnings("unchecked")
    public void lazy() throws Exception {
        WithLazy<Singleton> lazy = context.lookup(WithLazy.class, Singleton.class);
        assertSame(lazy.lazy.get(), lazy.lazy.get().instance);
    }
}