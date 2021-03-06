package com.github.ibessonov.cdi;

import com.github.ibessonov.cdi.annotations.*;
import com.github.ibessonov.cdi.exceptions.CdiException;
import com.github.ibessonov.cdi.internal.$CdiObject;
import com.github.ibessonov.cdi.internal.$Descriptor;
import org.junit.After;
import org.junit.Test;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static com.github.ibessonov.cdi.Context.auto;
import static com.github.ibessonov.cdi.enums.Scope.SINGLETON;
import static com.github.ibessonov.cdi.internal.$Descriptor.$;
import static com.github.ibessonov.cdi.internal.$Descriptor.$0;
import static com.github.ibessonov.cdi.util.CollectionUtil.array;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * @author ibessonov
 */
@SuppressWarnings("unchecked")
public class ContextTest extends CdiTest {

    @Override
    public Extension[] getExtensions() {
        return array(new TestExtension());
    }

    @After
    public void tearDown() {
        TestExtension.returned.clear();
    }

    public static class TestExtension implements Extension {

        public static final List<Object> returned = new ArrayList<>();

        private static final MethodHandle returnedHandle;

        static {
            try {
                returnedHandle = MethodHandles.lookup().in(TestExtension.class)
                        .findStatic(TestExtension.class, "returned", methodType(Object.class, Object.class));
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @Override
        public void register(Registrar registrar) {
            registrar.registerValueTransformer(NotNull.class, (annotation, clazz, object) ->
                Objects.requireNonNull(object, annotation.value())
            );
            registrar.registerValueTransformer(Trimmed.class, new ValueTransformer<Trimmed>() {
                @Override
                public Object transform(Trimmed annotation, Class<?> clazz, Object object) {
                    return ((String) object).trim();
                }

                @Override
                public boolean isApplicable(Class<?> clazz) {
                    return clazz == String.class;
                }
            });
            registrar.registerMethodTransformer(Traced.class, (annotation, method, handle) -> {
                Class<?> returnType = handle.type().returnType();
                return filterReturnValue(handle, returnedHandle.asType(methodType(returnType, returnType)));
            });
        }

        @SuppressWarnings("unused")
        static Object returned(Object value) {
            returned.add(value);
            return value;
        }
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface Traced {
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
        @Inject public void constructor(Singleton param) {
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
        GenericPair pair = context.$lookup($(GenericPair.class, $0(Integer.class), $0(Double.class)));

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
        InjectedClass injectedClass = context.$lookup($(InjectedClass.class, $0(String.class), $0(List.class)));
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
        assertEquals("", holder.shitty.value.value);
    }

    @Scoped static abstract class Lazy<T> implements Supplier<T> {

        private T value;

        @Override
        public T get() {
            T val = value;
            if (val == null) {
                val = value = init();
            }
            return val;
        }

        @NotNull abstract T init();
    }

    @Scoped static class WithLazy<T> {
        @Inject Lazy<T> lazy;
    }

    @Test
    public void lazy() {
        WithLazy<Singleton> withLazy = context.$lookup($(WithLazy.class, $0(Singleton.class)));
        assertSame(withLazy.lazy.get(), withLazy.lazy.get().instance);
    }

    @Scoped static class ValueTransformers {
        void notNull(@NotNull("passed object is null") Object object) {}
        @Trimmed String trimmed() { return "   str   "; }
    }

    @Test
    public void notNull() {
        ValueTransformers valueTransformers = context.lookup(ValueTransformers.class);
        try {
            valueTransformers.notNull(null);
            fail("NPE expected");
        } catch (NullPointerException npe) {
            assertEquals("passed object is null", npe.getMessage());
        }
    }

    @Test
    public void trimmed() {
        ValueTransformers valueTransformers = context.lookup(ValueTransformers.class);
        assertEquals("str", valueTransformers.trimmed());
    }

    @Scoped static class MethodTransformers {
        int value = 0;
        @Traced int nextInt() {
            return value++;
        }
    }

    @Test
    public void traced() {
        MethodTransformers methodTransformers = context.lookup(MethodTransformers.class);
        Integer value0 = methodTransformers.nextInt();
        Integer value1 = methodTransformers.nextInt();
        assertEquals(asList(value0, value1), TestExtension.returned);
    }

    @Test(timeout = 150)
    public void performance() {
        $Descriptor descriptor = $(InjectedClass.class, $0(String.class), $0(List.class));
        for (int i = 0; i < 100000; i++) {
            context.$lookup(descriptor);
        }
    }

    @Scoped static class Primitives {
        public void f(@NotNull boolean z, @NotNull byte b, @NotNull char  c, @NotNull short  s,
                      @NotNull int     i, @NotNull long j, @NotNull float f, @NotNull double d) {
        }
    }

    @Test
    public void primitives() {
        Primitives primitives = context.lookup(Primitives.class);
        primitives.f(false, (byte) 0, '\0', (short) 0, 0, 0L, 0f, 0d);
    }

    @Scoped static class InjectedParameter {
        public Class<String> f(@NotNull @Inject InjectedClass<String, List<String>> injected) {
            return injected.clazz;
        }
    }

    @Test
    public void injectedParameter() {
        InjectedParameter injectedParameter = context.lookup(InjectedParameter.class);
        assertEquals(String.class, injectedParameter.f(auto()));
    }
}