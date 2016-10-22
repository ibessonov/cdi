package org.ibess.cdi.runtime;

import org.ibess.cdi.Context;
import org.ibess.cdi.annotations.NotNull;
import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.enums.Scope;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.*;

/**
 * @author ibessonov
 */
@Ignore
public class AnnotationGeneratorTest {

    @Target(PARAMETER)
    @Retention(RUNTIME)
    public @interface TestingAnnotation {
        boolean booleanValue();
        boolean []booleanArrayValue();
        byte    byteValue();
        byte    []byteArrayValue();
        char    charValue();
        char    []charArrayValue();
        short   shortValue();
        short   []shortArrayValue();
        int     intValue();
        int     []intArrayValue();
        long    longValue();
        long    []longArrayValue();
        float   floatValue();
        float   []floatArrayValue();
        double  doubleValue();
        double  []doubleArrayValue();
        String  stringValue();
        String  []stringArrayValue();
        NotNull notNullValue();
        NotNull []notNullArrayValue();
        Scope   scopeValue();
        Scope   []scopeArrayValue();
        //Class   clazz();
        //Class   []classArray();
    }

    @Scoped
    static class Annotated {

        void f(
            @TestingAnnotation(
                booleanValue      =   true,
                booleanArrayValue = { false },
                byteValue         =   Byte.MAX_VALUE,
                byteArrayValue    = { Byte.MIN_VALUE },
                charValue         =   'x',
                charArrayValue    = { 'y' },
                shortValue        =   Short.MAX_VALUE,
                shortArrayValue   = { Short.MIN_VALUE },
                intValue          =   Integer.MAX_VALUE,
                intArrayValue     = { Integer.MIN_VALUE },
                longValue         =   Long.MAX_VALUE,
                longArrayValue    = { Long.MIN_VALUE },
                floatValue        =   Float.MAX_VALUE,
                floatArrayValue   = { Float.MIN_VALUE },
                doubleValue       =   Double.MAX_VALUE,
                doubleArrayValue  = { Double.MIN_VALUE },
                stringValue       =   "hello",
                stringArrayValue  = { "by" },
                notNullValue      =   @NotNull("single"),
                notNullArrayValue = { @NotNull("multiple") },
                scopeValue        =   Scope.SINGLETON,
                scopeArrayValue   = { Scope.STATELESS }
            ) Object object) {
        }
    }

    @Test
    public void completeTestWithContext() {
        Context.createContext(r -> r.registerValueTransformer(TestingAnnotation.class, (annotation, c, object) -> {
            assertFalse(Proxy.isProxyClass(annotation.getClass()));

            assertEquals(true, annotation.booleanValue());
            assertArrayEquals(new boolean[] { false }, annotation.booleanArrayValue());

            assertEquals(Byte.MAX_VALUE, annotation.byteValue());
            assertArrayEquals(new byte[] { Byte.MIN_VALUE }, annotation.byteArrayValue());

            assertEquals('x', annotation.charValue());
            assertArrayEquals(new char[] { 'y' }, annotation.charArrayValue());

            assertEquals(Short.MAX_VALUE, annotation.shortValue());
            assertArrayEquals(new short[] { Short.MIN_VALUE }, annotation.shortArrayValue());

            assertEquals(Integer.MAX_VALUE, annotation.intValue());
            assertArrayEquals(new int[] { Integer.MIN_VALUE }, annotation.intArrayValue());

            assertEquals(Long.MAX_VALUE, annotation.longValue());
            assertArrayEquals(new long[] { Long.MIN_VALUE }, annotation.longArrayValue());

            assertEquals(Float.MAX_VALUE, annotation.floatValue(), .0f);
            assertArrayEquals(new float[] { Float.MIN_VALUE }, annotation.floatArrayValue(), .0f);

            assertEquals(Double.MAX_VALUE, annotation.doubleValue(), .0d);
            assertArrayEquals(new double[] { Double.MIN_VALUE }, annotation.doubleArrayValue(), .0d);

            assertEquals("hello", annotation.stringValue());
            assertArrayEquals(new String[] { "by" }, annotation.stringArrayValue());

            assertEquals("single", annotation.notNullValue().value());
            assertEquals("multiple", annotation.notNullArrayValue()[0].value());

            assertFalse(Proxy.isProxyClass(annotation.notNullValue().value().getClass()));
            assertFalse(Proxy.isProxyClass(annotation.notNullArrayValue()[0].value().getClass()));

            assertEquals(Scope.SINGLETON, annotation.scopeValue());
            assertArrayEquals(new Scope[] { Scope.STATELESS }, annotation.scopeArrayValue());

            assertEquals(TestingAnnotation.class, annotation.annotationType());

            try {
                Method f = Annotated.class.getDeclaredMethod("f", Object.class);
                Parameter parameter = f.getParameters()[0];
                TestingAnnotation proxyAnnotation = parameter.getAnnotation(TestingAnnotation.class);

                assertTrue(Proxy.isProxyClass(proxyAnnotation.getClass()));

                assertEquals(annotation, proxyAnnotation);
                assertEquals(proxyAnnotation, annotation);

                assertEquals(proxyAnnotation.hashCode(), annotation.hashCode());
                assertEquals(proxyAnnotation.toString(), annotation.toString());
            } catch (Exception t) {
                fail(t.getMessage());
            }

            assertNull(object);
            return null;
        })).lookup(Annotated.class).f(null);
    }
}