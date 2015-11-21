package ibessonov.cdi;

import ibessonov.cdi.annotations.Constructor;
import ibessonov.cdi.annotations.Generic;
import ibessonov.cdi.annotations.Inject;
import ibessonov.cdi.annotations.Scoped;
import ibessonov.cdi.internal.$Generic;
import org.junit.Before;
import org.junit.Test;

import static ibessonov.cdi.Context.getTypeParameter;
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
        public Singleton instance;

        @Inject
        public GenericClass<String> generic;
    }

    @Generic
    @Scoped(STATELESS)
    public static class GenericClass<T> {
    }

    @Test
    public void lookup() {
        Singleton singleton = context.lookup(Singleton.class);

        assertEquals(singleton, singleton.instance);

        assertNotEquals(GenericClass.class, singleton.generic.getClass());
        assertTrue(singleton.generic instanceof $Generic);
        assertEquals(String.class, getTypeParameter(singleton.generic));
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
}