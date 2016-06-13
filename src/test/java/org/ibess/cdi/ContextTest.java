package org.ibess.cdi;

import org.ibess.cdi.annotations.*;
import org.ibess.cdi.exceptions.CdiException;
import org.ibess.cdi.internal.$CdiObject;
import org.ibess.cdi.internal.$Context;
import org.ibess.cdi.internal.$Descriptor;
import org.ibess.cdi.runtime.ContextImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static org.ibess.cdi.enums.Scope.SINGLETON;
import static org.ibess.cdi.internal.$Descriptor.$;
import static org.ibess.cdi.internal.$Descriptor.$0;
import static org.ibess.cdi.runtime.st.Dsl.*;
import static org.junit.Assert.*;

/**
 * @author ibessonov
 */
@SuppressWarnings("unchecked")
public class ContextTest {

    private $Context context;

    @Before
    public void setUp() {
        context = new ContextImpl(new TestExtension());
    }

    @After
    public void tearDown() {
        TestExtension.returned.clear();
    }

    public static class TestExtension implements Extension {

        public static final List<Object> returned = new ArrayList<>();

        @Override
        public void register(Registrar registrar) {
            registrar.registerValueTransformer(NotNull.class, (object, annotation) ->
                Objects.requireNonNull(object, annotation.value())
            );
            registrar.registerValueTransformer(Trimmed.class, (object, annotation) -> {
                if (object instanceof String) {
                    return object.toString().trim();
                } else {
                    throw new IllegalArgumentException(object.getClass().getName());
                }
            });
            registrar.registerMethodTransformer(Traced.class, (statement, method, annotation) ->
                $returnHook(statement,
                    $return($invokeStaticMethod($ofClass(TestExtension.class), $named("returned"),
                        $withParameterTypes(Object.class), $returnsNothing(), $withParameters($dup)
                    ))
                )
            );
        }

        @SuppressWarnings("unused")
        static void returned(Object value) {
            returned.add(value);
        }
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface Traced {
        String value() default "";
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

//    @Scoped static abstract class Lazy<T> implements Supplier<T> {
//
//        private T value;
//
//        @Override
//        public T get() {
//            T val = value;
//            if (val == null) {
//                val = value = init();
//            }
//            return val;
//        }
//
//        abstract T init();
//    }
//
//    @Scoped static class WithLazy<T> {
//        @Inject Lazy<T> lazy;
//    }
//
//    @Test
//    public void lazy() {
//        WithLazy<Singleton> lazy = (WithLazy) context.$lookup($(WithLazy.class, $0(Singleton.class)));
//        assertSame(lazy.lazy.get(), lazy.lazy.get().instance);
//    }

    @Scoped static class ValueTransformers {
        void notNull(@NotNull("passed object is null") Object object) {}
        @Trimmed String trimmed() { return "   str   "; }
    }

    @Test
    public void testNotNull() {
        ValueTransformers valueTransformers = context.lookup(ValueTransformers.class);
        try {
            valueTransformers.notNull(null);
            fail("NPE expected");
        } catch (NullPointerException npe) {
            assertEquals("passed object is null", npe.getMessage());
        }
    }

    @Test
    public void testTrimmed() {
        ValueTransformers valueTransformers = context.lookup(ValueTransformers.class);
        assertEquals("str", valueTransformers.trimmed());
    }

    @Scoped static class MethodTransformers {
        static Integer value = 0;
        @Traced Integer nextInt() {
            return value++;
        }
    }

    @Test
    public void testTraced() {
        MethodTransformers methodTransformers = context.lookup(MethodTransformers.class);
        Integer value0 = methodTransformers.nextInt();
        Integer value1 = methodTransformers.nextInt();
        assertEquals(asList(value0, value1), TestExtension.returned);
    }

    @Test(timeout = 150)
    public void zPerformance() {
        $Descriptor $ = $(InjectedClass.class, $0(String.class), $0(List.class));
        for (int i = 0; i < 100000; i++) {
            context.$lookup($);
        }
    }
}