package ibessonov.cdi.javac;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

/**
 * @author ibessonov
 */
class CdiFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    private final CompiledCode code;

    CdiFileManager(JavaFileManager fileManager, CompiledCode code) {
        super(fileManager);
        this.code = code;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) {
        return code;
    }
}
