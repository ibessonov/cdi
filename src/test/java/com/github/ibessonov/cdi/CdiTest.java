package com.github.ibessonov.cdi;

import com.github.ibessonov.cdi.internal.$Context;
import org.junit.Before;

import static com.github.ibessonov.cdi.util.CollectionUtil.array;
import static org.junit.Assert.assertEquals;

/**
 * @author ibessonov
 */
public class CdiTest {

    protected $Context context;

    @Before
    public void setUp() {
        context = ($Context) Context.createContext(getExtensions());
    }

    public Extension[] getExtensions() {
        return array();
    }

    public static void assertEqualsWithType(Object expected, Object actual) {
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected, actual);
    }
}
