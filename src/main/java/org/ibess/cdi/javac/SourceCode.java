package org.ibess.cdi.javac;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

import static javax.tools.JavaFileObject.Kind.SOURCE;

/**
 * @author ibessonov
 */
class SourceCode extends SimpleJavaFileObject {

    private final String content;

    public SourceCode(String className, String content) {
        super(URI.create("string:///" + className.replace('.', '/') + SOURCE.extension), SOURCE);
        this.content = content;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }
}
