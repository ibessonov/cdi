package org.ibess.cdi;

import org.ibess.cdi.annotations.NotNull;
import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.annotations.ex.TailRec;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author ibessonov
 */
public class TailRecTest extends CdiTest {

    @Scoped static class Factorial {

        @TailRec int factorial(int n, int acc) {
            if (n <= 1) return 1;
            return factorial(n - 1, acc * n);
        }
    }

    @Test
    public void factorial() {
        Factorial f0 = new Factorial();
        Factorial f1 = context.lookup(Factorial.class);

        for (int i = 0; i < 100; i++) {
            assertEquals(f0.factorial(i, 1), f1.factorial(i, 1));
        }

        try {
            f0.factorial(50_000, 1);
            fail("StackOverflowError expected");
        } catch (StackOverflowError ignored) {
        }

        f1.factorial(50_000, 1);
    }

    @Scoped static class BoxedFactorial {

        @TailRec @NotNull Integer factorial(@NotNull Integer n, @NotNull Integer acc) {
            if (n <= 1) return 1;
            return factorial(n - 1, acc * n);
        }
    }

    @Test
    public void annotatedFactorial() {
        BoxedFactorial f0 = new BoxedFactorial();
        BoxedFactorial f1 = context.lookup(BoxedFactorial.class);

        for (int i = 0; i < 100; i++) {
            assertEquals(f0.factorial(i, 1), f1.factorial(i, 1));
        }

        try {
            f0.factorial(50_000, 1);
            fail("StackOverflowError expected");
        } catch (StackOverflowError ignored) {
        }

        f1.factorial(50_000, 1);
    }
}
