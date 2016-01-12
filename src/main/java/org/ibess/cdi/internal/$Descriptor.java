package org.ibess.cdi.internal;

import java.util.Arrays;

/**
 * For internal purposes only. Must not be used outside of this library's code.
 * @author ibessonov
 */
public final class $Descriptor {

    private static final $Descriptor[] EMPTY = {};

    public  final Class c;
    public  final $Descriptor[] p;
    private final int h;

    private $Descriptor(Class c, $Descriptor[] p) {
        this.c = c;
        this.p = p;
        this.h = c.getName().hashCode() ^ Arrays.hashCode(p);
    }

    private $Descriptor(Class c) {
        this.c = c;
        this.p = EMPTY;
        this.h = c.getName().hashCode();
    }

    public static $Descriptor $(Class c, $Descriptor... p) {
        return new $Descriptor(c, p);
    }

    public static $Descriptor $0(Class c) {
        return new $Descriptor(c);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof $Descriptor)) return false;

        $Descriptor that = ($Descriptor) obj;
        return this.h == that.h && this.c == that.c && Arrays.equals(this.p, that.p);
    }

    @Override
    public int hashCode() {
        return h;
    }
}
