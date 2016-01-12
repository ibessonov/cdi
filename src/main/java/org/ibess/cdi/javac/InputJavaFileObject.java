package org.ibess.cdi.javac;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

import static javax.tools.JavaFileObject.Kind.SOURCE;

/**
 * @author ibessonov
 */
class InputJavaFileObject extends SimpleJavaFileObject {

    private final String content;

    public InputJavaFileObject(String className, String content) {
        super(URI.create("string:///" + className.replace('.', '/') + ".java"), SOURCE);
        this.content = content;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }
}
