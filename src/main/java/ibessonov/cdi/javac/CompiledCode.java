package ibessonov.cdi.javac;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

import static javax.tools.JavaFileObject.Kind.CLASS;

/**
 * @author ibessonov
 */
class CompiledCode extends SimpleJavaFileObject {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public CompiledCode(String className) {
        super(URI.create(className), CLASS);
    }

    @Override
    public OutputStream openOutputStream() {
        return out;
    }

    public byte[] toByteCode() {
        return out.toByteArray();
    }
}
