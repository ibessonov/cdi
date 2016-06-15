package org.ibess.cdi.runtime;

import org.ibess.cdi.Context;
import org.ibess.cdi.MethodTransformer;
import org.ibess.cdi.ValueTransformer;
import org.ibess.cdi.annotations.Constructor;
import org.ibess.cdi.annotations.Inject;
import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.exceptions.CdiException;
import org.ibess.cdi.exceptions.ImpossibleError;
import org.ibess.cdi.internal.$CdiObject;
import org.ibess.cdi.internal.$Context;
import org.ibess.cdi.internal.$Descriptor;
import org.ibess.cdi.internal.$Instantiator;
import org.ibess.cdi.runtime.st.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.reflect.Modifier.*;
import static org.ibess.cdi.enums.CdiErrorType.*;
import static org.ibess.cdi.enums.Scope.STATELESS;
import static org.ibess.cdi.runtime.CdiClassLoader.defineClass;
import static org.ibess.cdi.runtime.StCompiler.compile;
import static org.ibess.cdi.runtime.st.Dsl.*;

/**
 * @author ibessonov
 */
final class InheritorGenerator {

    private static final String CDI_SUFFIX = "$Cdi";
    private static final String I_SUFFIX   = "$I";

    private static final Method $lookup;
    private static final Method $unscoped;
    private static final Method $stateless;
    private static final Method $singleton;

    private static final Method $;
    private static final Method $0;

    private static final Method transform;

    private final ContextImpl context;
    private final Map<Class, ClassInfo> cache = new HashMap<>();

    private final String postfix;

    InheritorGenerator(ContextImpl context, String postfix) {
        this.context = context;
        this.postfix = postfix;
    }

    static {
        try {
            $lookup    = $Context.class.getDeclaredMethod("$lookup",    $Descriptor.class);
            $unscoped  = $Context.class.getDeclaredMethod("$unscoped",  Class.class);
            $stateless = $Context.class.getDeclaredMethod("$stateless", $Descriptor.class);
            $singleton = $Context.class.getDeclaredMethod("$singleton", $Descriptor.class);

            $  = $Descriptor.class.getDeclaredMethod("$",  Class.class, $Descriptor[].class);
            $0 = $Descriptor.class.getDeclaredMethod("$0", Class.class);

            transform = ValueTransformer.class.getDeclaredMethod("transform", Object.class, Class.class, Annotation.class);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public Class getSubclass(Class clazz) {
        ClassInfo ci = cache.get(clazz);
        if (ci == null || ci.compiledClass == null) {
            synchronized (InheritorGenerator.class) {
                if (ci == null) {
                    ci = cache.computeIfAbsent(clazz, this::getClassInfo);
                }
                if (ci.compiledClass == null) {
                    compileWithDependencies(ci, new HashSet<>());
                }
            }
        }
        return ci.compiledClass;
    }

    public ClassInfo getClassInfo(Class<?> clazz) {
        // validation
        if (isFinal(clazz.getModifiers())) {
            throw new CdiException(FINAL_SCOPED_CLASS, clazz.getCanonicalName());
        }
        if (clazz.getEnclosingClass() != null && !isStatic(clazz.getModifiers())) {
            // throw
        }
        Scoped scoped = clazz.getAnnotation(Scoped.class);
        if (scoped.value() != STATELESS && clazz.getTypeParameters().length > 0) {
            throw new CdiException(PARAMETERIZED_NON_STATELESS, clazz.getCanonicalName());
        }

        ClassInfo ci = new ClassInfo(clazz);

        for (Field field : ci.x.declaredFields) {
            if (injectable(field)) {
                validateFieldForInjection(ci, field);
            }
        }
        validateConstructor(ci);

//        concat(of(ci.methods), of(ci.declaredMethods)).distinct().forEach(method -> {
//            if (isAbstract(method.getModifiers())) implementMethod(ci, method);
//        });

        // generation
        String inheritorClassName = clazz.getName() + CDI_SUFFIX + postfix;
        String instantiatorClassName = inheritorClassName + "$" + I_SUFFIX;

        ci.x.stClasses.add($class($named(instantiatorClassName), $extends(Object.class), $implements($Instantiator.class),
            $withoutFields(), $withMethods(
                $method($named("<init>"), $withoutParameterTypes(), $returnsNothing(), $withBody(
                    $invoke($invokeSpecialMethod($ofClass(Object.class), $named("<init>"),
                        $withoutParameterTypes(), $returnsNothing(),
                        $on($this), $withoutParameters()
                    ))
                )),
                $method($named("$create"), $withParameterTypes($Context.class, $Descriptor[].class), $returns($CdiObject.class), $withBody(
                    $return($invokeSpecialMethod($ofClass(inheritorClassName), $named("<init>"),
                        $withParameterTypes($Context.class, $Descriptor[].class), $returnsNothing(),
                        $on($new(inheritorClassName)),
                        $withParameters($dup, $methodParam(0), $methodParam(1))
                    ))
                ))
            )
        ));

        // methods
        List<StMethod> methods = new ArrayList<>();
        methods.add($method($named("<init>"), $withParameterTypes($Context.class, $Descriptor[].class), $returnsNothing(), $withBody(
            $invoke($invokeSpecialMethod($ofClass(clazz), $named("<init>"),
                $withoutParameterTypes(), $returnsNothing(),
                $on($this), $withoutParameters()
            )),
            !ci.x.hasContext ? $noop()
                : $assignMyField($named("$c"), $withType($Context.class), $value($methodParam(0))),
            !ci.x.hasDescriptors ? $noop()
                : $assignMyField($named("$d"), $withType($Descriptor[].class), $value($methodParam(1)))
        )));

        methods.add($staticMethod($named("<clinit>"), $withoutParameterTypes(), $returnsNothing(), $withBody(
            $assignStatic($named("$i"), $ofClass(inheritorClassName), $withType($Instantiator.class), $value(
                $invokeSpecialMethod($ofClass(instantiatorClassName), $named("<init>"),
                    $withoutParameterTypes(), $returnsNothing(),
                    $on($new(instantiatorClassName)),
                    $withParameters($dup)
                )
            ))
        )));

        methods.add($method($named("$construct"), $withoutParameterTypes(), $returnsNothing(),
            $withBody(getConstructorBody(ci))
        ));

        // assume that validation is already done
        for (Method method : ci.x.declaredMethods) {
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            Class<?>[] parameterTypes = method.getParameterTypes();
            List<StStatement> statements = new ArrayList<>();
            int parametersCount = parameterAnnotations.length;
            for (int i = 0; i < parametersCount; i++) {
                List<Annotation> annotations = new ArrayList<>();
                for (Annotation parameterAnnotation : parameterAnnotations[i]) {
                    if (context.valueTransformerRegistered(parameterAnnotation.annotationType())) {
                        annotations.add(parameterAnnotation);
                    }
                }
                if (!annotations.isEmpty()) {
                    StExpression currentExpression = $methodParam(i);
                    for (Annotation annotation : annotations) {
                        ValueTransformer valueTransformer = context.getValueTransformer(annotation.annotationType());
                        String transformerField = ci.addStaticField(ValueTransformer.class, valueTransformer);
                        String annotationField  = ci.addStaticField(Annotation.class,  annotation);
                        currentExpression = $invokeInterfaceMethod(transform,
                            $on($myStaticField(transformerField, ValueTransformer.class)),
                            $withParameters(
                                currentExpression, $class(parameterTypes[i]), $myStaticField(annotationField, Annotation.class)
                            )
                        );
                        statements.add($assignMethodParam(i, $cast(parameterTypes[i], currentExpression)));
                    }
                }
            }
            List<Annotation> valueAnnotations = new ArrayList<>();
            List<Annotation> methodAnnotations = new ArrayList<>();
            for (Annotation methodAnnotation : method.getAnnotations()) {
                if (context.valueTransformerRegistered(methodAnnotation.annotationType())) {
                    valueAnnotations.add(methodAnnotation);
                }
                if (context.methodTransformerRegistered(methodAnnotation.annotationType())) {
                    methodAnnotations.add(methodAnnotation);
                }
            }
            if (!valueAnnotations.isEmpty() || !methodAnnotations.isEmpty() || !statements.isEmpty()) {
                List<StExpression> params = new ArrayList<>(parametersCount);
                for (int index = 0; index < parametersCount; index++) {
                    params.add($methodParam(index));
                }
                StExpression currentExpression = $invokeSpecialMethod(method, $on($this), $withParameters($expressions(params)));
                Class<?> returnType = method.getReturnType();
                StStatement methodStatement;
                if (returnType == void.class) {
                    methodStatement = $invoke((StMethodCallExpression) currentExpression);
                } else {
                    methodStatement = $return(currentExpression);
                }
                for (Annotation annotation : methodAnnotations) {
                    MethodTransformer methodTransformer = context.getMethodTransformer(annotation.annotationType());
                    if (methodTransformer != null) {
                        //noinspection unchecked
                        methodStatement = methodTransformer.transform(methodStatement, method, annotation);
                    }
                }
                if (returnType != void.class && !valueAnnotations.isEmpty()) {
                    StExpression hookExpression = $swap(); // that's tricky
                    for (Annotation annotation : valueAnnotations) {
                        ValueTransformer valueTransformer = context.getValueTransformer(annotation.annotationType());
                        String transformerField = ci.addStaticField(ValueTransformer.class, valueTransformer);
                        String annotationField  = ci.addStaticField(Annotation.class, annotation);
                        hookExpression = $invokeInterfaceMethod(transform,
                            $on($myStaticField(transformerField, ValueTransformer.class)),
                            $withParameters(
                                hookExpression, $class(returnType), $myStaticField(annotationField, Annotation.class)
                            )
                        );
                    }
                    methodStatement = $returnHook(methodStatement, $return($cast(returnType, hookExpression)));
                }
                statements.add(methodStatement);
                //noinspection ConfusingArgumentToVarargsMethod
                methods.add($method($named(method.getName()), $withParameterTypes(method.getParameterTypes()),
                    $returns(returnType), $withBody($statements(statements))
                ));
            }
        }

        // fields
        List<StField> fields = new ArrayList<>(3);
        fields.add($staticField($named("$i"), $withType($Instantiator.class)));
        for (ClassInfo.FieldInfo field : ci.x.staticFields) {
            fields.add($staticField($named(field.name), $withType(field.type)));
        }
        if (ci.x.hasContext) {
            fields.add($field($named("$c"), $withType($Context.class)));
        }
        if (ci.x.hasDescriptors) {
            fields.add($field($named("$d"), $withType($Descriptor[].class)));
        }

        ci.x.stClasses.add($class($named(inheritorClassName), $extends(clazz), $implements($CdiObject.class),
            $withFields($fields(fields)), $withMethods($methods(methods))
        ));

        return ci;
    }

    private static StStatement[] getConstructorBody(ClassInfo ci) {
        List<StStatement> statements = new ArrayList<>();
        for (Field field : ci.x.declaredFields) {
            if (injectable(field)) {
                statements.add($assign(field, $of($this), $value(getLookupExpression(ci, field.getGenericType()))));
            }
        }
        Method constructor = ci.x.constructor;
        if (constructor != null) {
            int parameterCount = constructor.getParameterCount();

            Type[] parameterTypes = constructor.getGenericParameterTypes();
            StExpression[] params = new StExpression[parameterCount];
            for (int i = 0; i < parameterCount; i++) {
                params[i] = getLookupExpression(ci, parameterTypes[i]);
            }

            statements.add($invoke($invokeVirtualMethod(constructor, $on($this), $withParameters(params))));
        }

        return $statements(statements);
    }

    private static StExpression getLookupExpression(ClassInfo ci, Type type) {
        if (type == Context.class) {
            return $myField($named("$c"), $withType($Context.class));
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class clazz = (Class) parameterizedType.getRawType();
            if (clazz == Class.class) {
                Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
                TypeVariable actualTypeVariable = (TypeVariable) actualTypeArgument;

                return $getField($ofClass($Descriptor.class), $named("c"), $withType(Class.class),
                                 $of(getTypeVariableDescriptor(ci, actualTypeVariable))
                );
            }
        }

        Class<?> rawClass = null;
        if (type instanceof Class) {
            rawClass = (Class) type;
        } else if (type instanceof ParameterizedType) {
            rawClass = (Class) ((ParameterizedType) type).getRawType();
        }

        StExpression context = $myField($named("$c"), $withType($Context.class));
        Method method;
        StExpression param;
        if (rawClass == null) {
            method = $lookup;
            param = getTypeVariableDescriptor(ci, (TypeVariable) type);
            rawClass = Object.class;
        } else {
            Scoped scoped = rawClass.getAnnotation(Scoped.class);
            if (scoped == null) {
                method = $unscoped;
                param = $class(rawClass);
            } else {
                switch (scoped.value()) {
                    case SINGLETON:
                        method = $singleton;
                        break;
                    case STATELESS:
                        method = $stateless;
                        break;
                    default:
                        throw new ImpossibleError();
                }
                param = getDescriptor(ci, type);
            }
        }

        return param instanceof StClassExpression // do not cast class object
             ? $invokeInterfaceMethod(method, $on(context), $withParameters(param))
             : $cast($toClass(rawClass), $invokeInterfaceMethod(method, $on(context), $withParameters(param)));
    }

    private static StExpression getTypeVariableDescriptor(ClassInfo ci, TypeVariable type) {
        return $arrayElement($of($myField($named("$d"), $withType($Descriptor[].class))),
                             $withIndex($int(ci.x.paramsIndex.get(type.getTypeName())))
        );
    }

    private static StExpression getDescriptor(ClassInfo ci, Type type) {
        if (type instanceof TypeVariable) {
            return getTypeVariableDescriptor(ci, (TypeVariable) type);
        } else {
            List<StExpression> params = new ArrayList<>(2);
            Method method;
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class clazz = (Class) parameterizedType.getRawType();

                params.add($class(clazz));
                if (clazz.isAnnotationPresent(Scoped.class)) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length == 0) {
                        method = $0;
                    } else {
                        boolean optimize = true;
                        for (int i = 0, length = actualTypeArguments.length; i < length; i++) {
                            Type actualTypeArgument = actualTypeArguments[i];
                            boolean matches = (actualTypeArgument instanceof TypeVariable)
                                    && (i == ci.x.paramsIndex.get(actualTypeArgument.getTypeName()));
                            if (!matches) {
                                optimize = false;
                                break;
                            }
                        }

                        if (optimize) {
                            params.add($myField($named("$d"), $withType($Descriptor[].class)));
                        } else {
                            StExpression[] elements = new StExpression[actualTypeArguments.length];
                            for (int i = 0, length = actualTypeArguments.length; i < length; i++) {
                                elements[i] = getDescriptor(ci, actualTypeArguments[i]);
                            }
                            params.add($array($Descriptor.class, elements));
                        }
                        method = $;
                    }
                } else {
                    method = $0;
                }
            } else if (type instanceof Class) {
                params.add($class((Class) type));
                method = $0;
            } else {
                throw new ImpossibleError();
            }
            return $invokeStaticMethod(method, $expressions(params));
        }
    }

    private void compileWithDependencies(ClassInfo ci, Set<ClassInfo> dejaVu) {
        if (ci.compiledClass != null) return;
        if (dejaVu.contains(ci)) return;

        dejaVu.add(ci);
        for (Class clazz : ci.x.dependsOn) {
            ClassInfo info = cache.computeIfAbsent(clazz, this::getClassInfo);
            compileWithDependencies(info, dejaVu);
        }
        dejaVu.remove(ci);

        for (StClass stClass : ci.x.stClasses) {
            ci.compiledClass = defineClass(compile(stClass));
        }

        try {
            for (ClassInfo.FieldInfo field : ci.x.staticFields) {
                ci.compiledClass.getDeclaredField(field.name).set(null, field.value);
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new ImpossibleError(e);
        }

        ci.dispose();
    }

    private static void validateConstructor(ClassInfo ci) {
        Method constructor = null;
        for (Method method : ci.x.declaredMethods) {
            if (method.isAnnotationPresent(Constructor.class)) {
                if (constructor != null) {
                    throw new CdiException(TOO_MANY_CONSTRUCTORS, ci.x.originalName);
                }
                constructor = method;
            }
        }
        if (constructor == null) return;
        ci.x.constructor = constructor;

        validateConstructor(ci, constructor);
        for (Type parameterType : constructor.getGenericParameterTypes()) {
            validateLookup(ci, parameterType);
        }
    }

    private static void validateLookup(ClassInfo ci, Type type) {
        if (type == Class.class) {
            throw new CdiException(CLASS_INJECTION, ci.x.originalName, type.getTypeName());
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() == Class.class) {
                Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
                if (!(actualTypeArgument instanceof TypeVariable)) {
                    throw new CdiException(CLASS_INJECTION, ci.x.originalName, type.getTypeName());
                }
                ci.x.hasDescriptors = true;
                return;
            }
        }

        ci.x.hasContext = true;
        validateType(ci, type);
    }

    private static boolean injectable(Field field) {
        return field.isAnnotationPresent(Inject.class);
    }

    private static void validateFieldForInjection(ClassInfo ci, Field field) {
        if (isPrivate(field.getModifiers())) {
            throw new CdiException(PRIVATE_FIELD_INJECTION, ci.x.originalName, field.getName());
        }
        if (isFinal(field.getModifiers())) {
            throw new CdiException(FINAL_FIELD_INJECTION, ci.x.originalName, field.getName());
        }
        validateLookup(ci, field.getGenericType());
    }

    private static void validateConstructor(ClassInfo ci, Method constructor) {
        Class[] exceptions = constructor.getExceptionTypes();
        for (Class exceptionClass : exceptions) {
            if (!RuntimeException.class.isAssignableFrom(exceptionClass) && !Error.class.isAssignableFrom(exceptionClass)) {
                throw new CdiException(CONSTRUCTOR_THROWS_EXCEPTION, ci.x.originalName, exceptionClass.getCanonicalName());
            }
        }
        if (isPrivate(constructor.getModifiers())) {
            throw new CdiException(CONSTRUCTOR_IS_PRIVATE, ci.x.originalName);
        }
        if (isAbstract(constructor.getModifiers())) {
            throw new CdiException(CONSTRUCTOR_IS_ABSTRACT, ci.x.originalName);
        }
        if (constructor.getTypeParameters().length > 0) {
            throw new CdiException(CONSTRUCTOR_IS_GENERIC, ci.x.originalName);
        }
    }

    private static void validateType(ClassInfo ci, Type type) {
        if (type instanceof Class) {
            Class clazz = (Class) type; // raw type should be validated in future
            if (clazz.isAnnotationPresent(Scoped.class)) {
                TypeVariable[] params = clazz.getTypeParameters();
                if (params.length > 0) {
                    throw new CdiException(GENERIC_PARAMETERS_COUNT_MISMATCH, clazz.getCanonicalName(), 0, params.length);
                }
                ci.x.dependsOn.add(clazz);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Class clazz = (Class) pType.getRawType(); // raw type should be validated in future, but not parameterized type. That would cause infinite recursion in some cases
            if (clazz.isAnnotationPresent(Scoped.class)) {
                for (Type param : pType.getActualTypeArguments()) {
                    validateType(ci, param);
                }
                ci.x.dependsOn.add(clazz);
            }
        } else if (type instanceof TypeVariable) {
            ci.x.hasDescriptors = true;
            // remember it for future checks
//            TypeVariable typeVariable = (TypeVariable) type;
//            int index = ci.paramsIndex.get(typeVariable.getName());
        } else if (type instanceof WildcardType) {
            throw new CdiException(WILDCARD_TYPE_PARAMETER, ci.x.originalName);
        } else if (type instanceof GenericArrayType) {
            throw new CdiException(ARRAY_TYPE_PARAMETER, ci.x.originalName);
        }
    }

    public static class ClassInfo {
        public Class compiledClass = null;

        public Temp x;

        public static class Temp {
            public final String originalName;
            public final Field[] declaredFields;
            public final Method[] declaredMethods;
            public final Method[] methods;
            public final Map<String, Integer> paramsIndex = new HashMap<>();
            public final Set<Class> dependsOn = new HashSet<>();
            public Method constructor = null;
            public boolean hasContext = false;
            public boolean hasDescriptors = false;
            public final List<StClass> stClasses = new ArrayList<>();
            public final List<FieldInfo> staticFields = new ArrayList<>();

            public final AtomicInteger counter = new AtomicInteger();

            public Temp(String originalName, Field[] declaredFields, Method[] declaredMethods, Method[] methods) {
                this.originalName = originalName;
                this.declaredFields = declaredFields;
                this.declaredMethods = declaredMethods;
                this.methods = methods;
            }
        }

        public static class FieldInfo {
            public final String name;
            public final Class<?> type;
            public final Object value;

            public FieldInfo(String name, Class<?> type, Object value) {
                this.name = name;
                this.type = type;
                this.value = value;
            }
        }

        public ClassInfo(Class clazz) {
            this.x = new Temp(clazz.getCanonicalName(), clazz.getDeclaredFields(),
                              clazz.getDeclaredMethods(), clazz.getMethods());

            TypeVariable[] params = clazz.getTypeParameters();
            for (int i = 0; i < params.length; i++) {
                this.x.paramsIndex.put(params[i].getName(), i);
            }
        }

        public String addStaticField(Class<?> type, Object value) {
            String name = "$auto" + x.counter.getAndIncrement();
            x.staticFields.add(new FieldInfo(name, type, value));
            return name;
        }

        public void dispose() {
            x = null;
        }
    }
}
