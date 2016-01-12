package org.ibess.cdi.javac;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ibessonov
 */
class CdiFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    final Map<String, OutputJavaFileObject> code = new HashMap<>();

    CdiFileManager(JavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) {
        return code.computeIfAbsent(className, OutputJavaFileObject::new);
    }
}
