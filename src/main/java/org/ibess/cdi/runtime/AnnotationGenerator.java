package org.ibess.cdi.runtime;

import org.ibess.cdi.exceptions.ImpossibleError;
import org.ibess.cdi.runtime.st.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.ibess.cdi.runtime.CdiClassLoader.defineClass;
import static org.ibess.cdi.runtime.StCompiler.compile;
import static org.ibess.cdi.runtime.st.Dsl.*;
import static org.ibess.cdi.util.BoxingUtil.box;
import static org.ibess.cdi.util.BoxingUtil.unbox;

/**
 * @author ibessonov
 */
final class AnnotationGenerator {

    private static final String ANNOTATION_SUFFIX = "$Strict";

    private static final ConcurrentMap<Class, Info> cache = new ConcurrentHashMap<>();

    private static class Info {

        public final Class clazz;
        public final Method[] methods; // we should keep methods in the same order

        private Info(Class clazz, Method[] methods) {
            this.clazz = clazz;
            this.methods = methods;
        }
    }

    public static StExpression getExpression(Annotation annotation) {
        Class<? extends Annotation> clazz = annotation.annotationType();
        Info info = getInfo(clazz);
        int length = info.methods.length;

        Class<?>[] parameterTypes = new Class<?>[length + 2];
        for (int i = 0; i < length; i++) {
            parameterTypes[i] = info.methods[i].getReturnType();
        }
        parameterTypes[length] = int.class;
        parameterTypes[length + 1] = String.class;

        StExpression[] parameters = new StExpression[length + 3];
        parameters[0] = _dup;
        try {
            for (int i = 0; i < length; i++) {
                parameters[i + 1] = getExpression0(info.methods[i].invoke(annotation));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ImpossibleError(e);
        }
        parameters[length + 1] = _int(annotation.hashCode());
        parameters[length + 2] = _string(annotation.toString());

        return _invokeSpecialMethod(_ofClass(info.clazz), _named("<init>"),
            _withParameterTypes(parameterTypes), _returnsNothing(),
            _on(_new(info.clazz)), _withParameters(parameters)
        );
    }

    private static Info getInfo(Class<? extends Annotation> clazz) {
        return cache.computeIfAbsent(clazz, AnnotationGenerator::generateSubclassInfo);
    }

    private static Info generateSubclassInfo(Class<? extends Annotation> clazz) {
        String className = clazz.getName() + ANNOTATION_SUFFIX;
        Method[] declaredMethods = clazz.getDeclaredMethods();
        int length = declaredMethods.length;

        StField[] fields = new StField[length + 2]; // $hash, $str
        for (int i = 0; i < length; i++) {
            Method declaredMethod = declaredMethods[i];
            fields[i] = _field(declaredMethod.getName(), declaredMethod.getReturnType());
        }
        // it's safer to store these values rather then cloning algorithms that may change over time
        fields[length    ] = _field("hashCode", int.class);
        fields[length + 1] = _field("toString", String.class);

        Class[] fieldTypes = new Class[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldTypes[i] = fields[i].type;
        }

        StMethod[] methods = new StMethod[length + 5]; // hashCode, <init>, equals, toString, annotationType
        for (int i = 0; i < length; i++) {
            methods[i] = _overrideMethod(declaredMethods[i], _withBody(
                _return(_myField(_named(fields[i].name)))
            ));
        }
        methods[length] = _method(_named("hashCode"), _withoutParameterTypes(), _returns(int.class), _withBody(
            _return(_myField(_named("hashCode")))
        ));
        methods[length + 1] = _method(_named("toString"), _withoutParameterTypes(), _returns(String.class), _withBody(
            _return(_myField(_named("toString")))
        ));
        methods[length + 2] = _method(_named("<init>"), _withParameterTypes(fieldTypes), _returnsNothing(), _withBody(
            _invoke(_invokeSpecialMethod(_ofClass(Object.class), _named("<init>"),
                _withoutParameterTypes(), _returnsNothing(),
                _on(_this), _withoutParameters()
            )),
            _scope(methodBodyForInit(fields))
        ));
        methods[length + 3] = _method(_named("equals"), _withParameterTypes(Object.class), _returns(boolean.class), _withBody(
            _if(_invokeSpecialMethod(_ofClass(Object.class), _named("equals"),
                _withParameterTypes(Object.class), _returns(boolean.class),
                _on(_this), _withParameters(_methodParam(0))
            ), _then(_return(true))),
            _ifNot(_invokeVirtualMethod(_ofClass(Class.class), _named("isInstance"),
                _withParameterTypes(Object.class), _returns(boolean.class),
                _on(_class(clazz)), _withParameters(_methodParam(0))
            ), _then(_return(false))),
            _scope(methodBodyForEquals(declaredMethods)),
            _return(true)
        ));
        methods[length + 4] = _method(_named("annotationType"), _withoutParameterTypes(), _returns(Class.class), _withBody(
            _return(_class(clazz))
        ));

        StClass stClass = _class(_named(className), _extends(Object.class), _implements(clazz),
            _withFields(fields), _withMethods(methods)
        );
        return new Info(defineClass(compile(stClass)), declaredMethods);
    }

    private static StStatement[] methodBodyForInit(StField[] fields) {
        StStatement[] statements = new StStatement[fields.length];
        for (int i = 0; i < fields.length; i++) {
            StField field = fields[i];
            statements[i] = _assignMyField(_named(field.name), _withType(field.type), _value(_methodParam(i)));
        }
        return statements;
    }

    // dumb non-effective implementation
    // still better then the one from AnnotationInvocationHandler
    private static StStatement[] methodBodyForEquals(Method[] methods) {
        StStatement[] statements = new StStatement[methods.length];

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            Class<?> type = method.getReturnType();
            Class<?> annotation = method.getDeclaringClass();
            String name = method.getName();

            StExpression parameterField = _invokeInterfaceMethod(
                method, _on(_cast(_toClass(annotation), _methodParam(0))), _withoutParameters()
            );
            StExpression myField = _myField(_named(name));

            if (type.isPrimitive()) {
                parameterField = box(type, parameterField);
                myField = box(type, myField);
            }

            if (type.isArray()) {
                Class<?> arrayType = type.getComponentType().isPrimitive() ? type : Object[].class;
                statements[i] = _ifNot(_invokeStaticMethod(_ofClass(Arrays.class), _named("equals"),
                    _withParameterTypes(arrayType, arrayType), _returns(boolean.class),
                    _withParameters(parameterField, myField)
                ), _then(_return(false)));
            } else {
                statements[i] = _ifNot(_invokeVirtualMethod(_ofClass(Object.class), _named("equals"),
                    _withParameterTypes(Object.class), _returns(boolean.class),
                    _on(parameterField), _withParameters(myField)
                ), _then(_return(false)));
            }
        }
        return statements;
    }

    private static StExpression getExpression0(Object object) {
        Class<?> clazz = unbox(object.getClass());
        if (Annotation.class.isAssignableFrom(clazz)) {
            return getExpression((Annotation) object);
        }
        if (clazz.isArray()) {
            Class<?> componentType = clazz.getComponentType();
            int length = Array.getLength(object);
            StExpression[] elements = new StExpression[length];
            if (componentType.isPrimitive()) {
                switch (componentType.getName()) {
                    case "boolean":
                        for (int i = 0; i < length; i++) elements[i] = getExpression0(Array.getBoolean(object, i));
                        break;
                    case "byte":
                        for (int i = 0; i < length; i++) elements[i] = getExpression0(Array.getByte(object, i));
                        break;
                    case "char":
                        for (int i = 0; i < length; i++) elements[i] = getExpression0(Array.getChar(object, i));
                        break;
                    case "short":
                        for (int i = 0; i < length; i++) elements[i] = getExpression0(Array.getShort(object, i));
                        break;
                    case "int":
                        for (int i = 0; i < length; i++) elements[i] = getExpression0(Array.getInt(object, i));
                        break;
                    case "long":
                        for (int i = 0; i < length; i++) elements[i] = getExpression0(Array.getLong(object, i));
                        break;
                    case "float":
                        for (int i = 0; i < length; i++) elements[i] = getExpression0(Array.getFloat(object, i));
                        break;
                    case "double":
                        for (int i = 0; i < length; i++) elements[i] = getExpression0(Array.getDouble(object, i));
                        break;
                }
            } else {
                for (int i = 0; i < length; i++) elements[i] = getExpression0(Array.get(object, i));
            }
            return _array(componentType, elements);
        }
        if (clazz.isPrimitive()) {
            switch (clazz.getName()) {
                case "boolean": return _bool((Boolean) object);
                case "byte"   : return _byte((Byte) object);
                case "char"   : return _char((Character) object);
                case "short"  : return _short((Short) object);
                case "int"    : return _int((Integer) object);
                case "long"   : return _long((Long) object);
                case "float"  : return _float((Float) object);
                case "double" : return _double((Double) object);
            }
        }
        if (clazz == String.class) {
            return _string((String) object);
        }
        if (clazz.isEnum()) {
            Enum enumObject = (Enum) object;
            Class enumClass = enumObject.getDeclaringClass();
            return _getStaticField(_ofClass(enumClass), _named(enumObject.name()), _withType(enumClass));
        }
        throw new ImpossibleError("Impossible value in annotation. Value class is " + clazz.getName());
    }
}
