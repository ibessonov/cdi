package org.ibess.cdi.reflection;

import org.ibess.cdi.annotations.Scoped;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author ibessonov
 */
public class InheritorGeneratorTest {

    @Scoped
    static class ParameterClass {
    }

    @Test
    public void generate() {
        Class<ParameterClass> clazz = ParameterClass.class;

        Class<? extends ParameterClass> subclass = InheritorGenerator.getSubclass(clazz);
        assertNotSame(clazz, subclass);
        assertTrue(clazz.isAssignableFrom(subclass));
    }
}

