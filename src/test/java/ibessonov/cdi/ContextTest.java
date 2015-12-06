package ibessonov.cdi;

import ibessonov.cdi.annotations.Constructor;
import ibessonov.cdi.annotations.Generic;
import ibessonov.cdi.annotations.Inject;
import ibessonov.cdi.annotations.Scoped;
import ibessonov.cdi.internal.$Generic;
import org.junit.Before;
import org.junit.Test;

import static ibessonov.cdi.util.Cdi.getTypeParameter;
import static ibessonov.cdi.enums.Scope.SINGLETON;
import static ibessonov.cdi.enums.Scope.STATELESS;
import static org.junit.Assert.*;

/**
 * @author ibessonov
 */
public class ContextTest {

    private Context context;

    @Before
    public void setUp() {
        context = new Context();
    }

    @Scoped(SINGLETON)
    public static class Singleton {

        @Inject
        Singleton instance;

        @Inject
        GenericClass<String> generic;
    }

    @Generic
    @Scoped(STATELESS)
    public static class GenericClass<T> {

        @Inject
        GenericInnerClass<T> value;
    }

    @Generic
    @Scoped(STATELESS)
    public static class GenericInnerClass<T> {
    }

    @Test
    public void lookup() {
        Singleton singleton = context.lookup(Singleton.class);

        assertEquals(singleton, singleton.instance);

        assertNotEquals(GenericClass.class, singleton.generic.getClass());
        assertTrue(singleton.generic instanceof $Generic);
        assertEquals(String.class, getTypeParameter(singleton.generic));

        assertNotEquals(GenericInnerClass.class, singleton.generic.value.getClass());
        assertTrue(singleton.generic.value instanceof $Generic);
        assertEquals(String.class, getTypeParameter(singleton.generic.value));
    }

    @Scoped(STATELESS)
    public static class WithConstructor {

        public int value;

        @Constructor
        public void constructor() {
            value = 15;
        }
    }

    @Test
    public void constructor() {
        WithConstructor withConstructor = context.lookup(WithConstructor.class);

        assertEquals(15, withConstructor.value);
    }

    @Scoped(STATELESS)
    public static class WithContext {

        @Inject
        Context context;
    }

    @Test
    public void context() {
        WithContext withContext = context.lookup(WithContext.class);

        assertSame(context, withContext.context);
    }
}