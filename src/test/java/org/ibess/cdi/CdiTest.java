package org.ibess.cdi;

import org.ibess.cdi.internal.$Context;
import org.junit.Before;

import static org.ibess.cdi.util.CollectionUtil.array;
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
