package ibessonov.cdi.javac;

import org.junit.Test;

import java.util.function.IntSupplier;

import static ibessonov.cdi.reflection.ReflectionUtil.newInstance;
import static org.junit.Assert.assertEquals;

public class JavaCTest {

    @Test
    public void compile() {
        String sourceCode =
                "package ibessonov.cdi.javac;\n" +
                "public class HelloClass implements java.util.function.IntSupplier {\n" +
                "   @Override public int getAsInt() { return 37; }\n" +
                "}";

        Class<?> helloClass = JavaC.compile("ibessonov.cdi.javac.HelloClass", sourceCode);

        IntSupplier supplier = (IntSupplier) newInstance(helloClass);
        assertEquals(37, supplier.getAsInt());
    }

    public static class ForTest {
        int a = 0;
    }

    @Test
    public void packageProtected() throws Exception {
        String sourceCode =
                "package ibessonov.cdi.javac;\n" +
                "public class ForTest0 extends ibessonov.cdi.javac.JavaCTest.ForTest {\n" +
                "   { a = 10; }\n" +
                "}";

        Class<?> clazz = JavaC.compile("ibessonov.cdi.javac.ForTest0", sourceCode);
        ForTest forTest = (ForTest) newInstance(clazz);
        assertEquals(10, forTest.a);
    }
}
