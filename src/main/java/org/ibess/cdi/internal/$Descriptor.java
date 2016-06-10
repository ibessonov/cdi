package org.ibess.cdi.internal;

/**
 * For internal purposes only. Must not be used outside of this library's code.
 * @author ibessonov
 */
public final class $Descriptor {

    private static final $Descriptor[] EMPTY = {};

    public  final Class<?> c;
    public  final $Descriptor[] p;
    private final int h;

    private $Descriptor(Class c, $Descriptor[] p) {
        this.c = c;
        this.p = p;
        this.h = c.getName().hashCode() ^ hashCode(p);
    }

    private $Descriptor(Class c) {
        this.c = c;
        this.p = EMPTY;
        this.h = c.getName().hashCode() ^ 1;
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
        return this.h == that.h && this.c == that.c && equals(this.p, that.p);
    }

    @Override
    public int hashCode() {
        return h;
    }

    // hashCode method without null checks
    private static int hashCode($Descriptor p[]) {
        int result = 1;
        for ($Descriptor d : p) {
            result = 31 * result + d.h;
        }
        return result;
    }

    // equals method without null checks
    private static boolean equals($Descriptor[] l, $Descriptor[] r) {
        if (l == r) return true;
        int length = l.length;
        if (r.length != length) return false;
        for (int i = 0; i < length; i++) {
            if (l[i].h != r[i].h || l[i].c != r[i].c) return false;
        }
        for (int i = 0; i < length; i++) {
            if (!equals(l[i].p, r[i].p)) return false;
        }
        return true;
    }
}
