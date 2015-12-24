package ibessonov.cdi.reflection;

import java.util.Arrays;

/**
 * @author ibessonov
 */
public final class Descriptor<T> {

    private static final Descriptor<?>[] EMPTY = {};

    public  final Class<T> c;
    public  final Descriptor<?>[] p;
    private final int h;

    private Descriptor(Class<T> c, Descriptor<?>[] p) {
        this.c = c;
        this.p = p;
        this.h = c.hashCode() ^ Arrays.hashCode(p);
    }

    public static <T> Descriptor<T> $$(Class<T> c, Descriptor<?> ... p) {
        return new Descriptor<>(c, p);
    }

    public static <T> Descriptor<T> $$(Class<T> c) {
        return new Descriptor<>(c, EMPTY);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Descriptor)) return false;

        Descriptor that = (Descriptor) obj;
        return this.h == that.h && this.c == that.c && Arrays.equals(this.p, that.p);
    }

    @Override
    public int hashCode() {
        return h;
    }
}
