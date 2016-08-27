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
import static org.ibess.cdi.runtime.st.BoxingUtil.box;
import static org.ibess.cdi.runtime.st.BoxingUtil.unbox;
import static org.ibess.cdi.runtime.st.Dsl.*;

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
        parameters[0] = $dup;
        try {
            for (int i = 0; i < length; i++) {
                parameters[i + 1] = getExpression0(info.methods[i].invoke(annotation));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ImpossibleError(e);
        }
        parameters[length + 1] = $int(annotation.hashCode());
        parameters[length + 2] = $string(annotation.toString());

        return $invokeSpecialMethod($ofClass(info.clazz), $named("<init>"),
            $withParameterTypes(parameterTypes), $returnsNothing(),
            $on($new(info.clazz)), $withParameters(parameters)
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
            fields[i] = $field(declaredMethod.getName(), declaredMethod.getReturnType());
        }
        // it's safer to store these values rather then cloning algorithms that may change over time
        fields[length    ] = $field("$hash", int.class);
        fields[length + 1] = $field("$str", String.class);

        Class[] fieldTypes = new Class[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldTypes[i] = fields[i].type;
        }

        StMethod[] methods = new StMethod[length + 5]; // hashCode, <init>, equals, toString, annotationType
        for (int i = 0; i < length; i++) {
            StField field = fields[i];
            methods[i] = $method($named(field.name), $withoutParameterTypes(), $returns(field.type), $withBody(
                $return($myField($named(field.name)))
            ));
        }
        methods[length] = $method($named("hashCode"), $withoutParameterTypes(), $returns(int.class), $withBody(
            $return($myField($named("$hash")))
        ));
        methods[length + 1] = $method($named("toString"), $withoutParameterTypes(), $returns(String.class), $withBody(
            $return($myField($named("$str")))
        ));
        methods[length + 2] = $method($named("<init>"), $withParameterTypes(fieldTypes), $returnsNothing(), $withBody(
            $invoke($invokeSpecialMethod($ofClass(Object.class), $named("<init>"),
                $withoutParameterTypes(), $returnsNothing(),
                $on($this), $withoutParameters()
            )),
            $scope(methodBodyForInit(fields))
        ));
        methods[length + 3] = $method($named("equals"), $withParameterTypes(Object.class), $returns(boolean.class), $withBody(
            $if($invokeSpecialMethod($ofClass(Object.class), $named("equals"),
                $withParameterTypes(Object.class), $returns(boolean.class),
                $on($this), $withParameters($methodParam(0))
            ), $then($return(true))),
            $ifNot($invokeVirtualMethod($ofClass(Class.class), $named("isInstance"),
                $withParameterTypes(Object.class), $returns(boolean.class),
                $on($class(clazz)), $withParameters($methodParam(0))
            ), $then($return(false))),
            $scope(methodBodyForEquals(declaredMethods)),
            $return(true)
        ));
        methods[length + 4] = $method($named("annotationType"), $withoutParameterTypes(), $returns(Class.class), $withBody(
            $return($class(clazz))
        ));

        StClass stClass = $class($named(className), $extends(Object.class), $implements(clazz),
            $withFields(fields), $withMethods(methods)
        );
        return new Info(defineClass(compile(stClass)), declaredMethods);
    }

    private static StStatement[] methodBodyForInit(StField[] fields) {
        StStatement[] statements = new StStatement[fields.length];
        for (int i = 0; i < fields.length; i++) {
            StField field = fields[i];
            statements[i] = $assignMyField($named(field.name), $withType(field.type), $value($methodParam(i)));
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

            StExpression parameterField = $invokeInterfaceMethod(
                method, $on($cast($toClass(annotation), $methodParam(0))), $withoutParameters()
            );
            StExpression myField = $myField($named(name));

            if (type.isPrimitive()) {
                parameterField = box(type, parameterField);
                myField = box(type, myField);
            }

            if (type.isArray()) {
                Class<?> arrayType = type.getComponentType().isPrimitive() ? type : Object[].class;
                statements[i] = $ifNot($invokeStaticMethod($ofClass(Arrays.class), $named("equals"),
                    $withParameterTypes(arrayType, arrayType), $returns(boolean.class),
                    $withParameters(parameterField, myField)
                ), $return(false));
            } else {
                statements[i] = $ifNot($invokeVirtualMethod($ofClass(Object.class), $named("equals"),
                    $withParameterTypes(Object.class), $returns(boolean.class),
                    $on(parameterField), $withParameters(myField)
                ), $return(false));
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
            return $array(componentType, elements);
        }
        if (clazz.isPrimitive()) {
            switch (clazz.getName()) {
                case "boolean": return $bool((Boolean) object);
                case "byte"   : return $byte((Byte) object);
                case "char"   : return $char((Character) object);
                case "short"  : return $short((Short) object);
                case "int"    : return $int((Integer) object);
                case "long"   : return $long((Long) object);
                case "float"  : return $float((Float) object);
                case "double" : return $double((Double) object);
            }
        }
        if (clazz == String.class) {
            return $string((String) object);
        }
        if (clazz.isEnum()) {
            Enum enumObject = (Enum) object;
            Class enumClass = enumObject.getDeclaringClass();
            return $getStaticField($ofClass(enumClass), $named(enumObject.name()), $withType(enumClass));
        }
        throw new ImpossibleError("Impossible value in annotation. Value class is " + clazz.getName());
    }
}
