package org.ibess.cdi.runtime;

import org.ibess.cdi.Context;
import org.ibess.cdi.annotations.Inject;
import org.ibess.cdi.annotations.Scoped;
import org.ibess.cdi.enums.Scope;
import org.ibess.cdi.exceptions.CdiException;
import org.ibess.cdi.exceptions.ImpossibleError;
import org.ibess.cdi.internal.$CdiObject;
import org.ibess.cdi.internal.$Context;
import org.ibess.cdi.internal.$Descriptor;
import org.ibess.cdi.internal.$Instantiator;
import org.ibess.cdi.runtime.st.*;
import org.ibess.cdi.util.CollectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Stream.concat;
import static org.ibess.cdi.enums.CdiErrorType.*;
import static org.ibess.cdi.enums.Scope.STATELESS;
import static org.ibess.cdi.runtime.CdiClassLoader.defineClass;
import static org.ibess.cdi.runtime.StCompiler.compile;
import static org.ibess.cdi.runtime.st.Dsl.*;
import static org.ibess.cdi.util.ScopedAnnotationCache.getScope;

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

    static {
        try {
            $lookup    = $Context.class.getDeclaredMethod("$lookup",    $Descriptor.class);
            $unscoped  = $Context.class.getDeclaredMethod("$unscoped",  Class.class);
            $stateless = $Context.class.getDeclaredMethod("$stateless", $Descriptor.class);
            $singleton = $Context.class.getDeclaredMethod("$singleton", $Descriptor.class);

            $  = $Descriptor.class.getDeclaredMethod("$",  Class.class, $Descriptor[].class);
            $0 = $Descriptor.class.getDeclaredMethod("$0", Class.class);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final ContextImpl context;
    private final Map<Class, ClassInfo> registry = new HashMap<>();

    private final String contextId;

    InheritorGenerator(ContextImpl context, String contextId) {
        this.context = context;
        this.contextId = contextId;
    }

    public Class<?> getSubclass(Class<?> clazz) {
        ClassInfo ci = registry.get(clazz);
        if (ci == null || ci.compiledClass == null) {
            synchronized (InheritorGenerator.class) {
                if (ci == null) {
                    ci = registry.computeIfAbsent(clazz, this::getClassInfo);
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
        Scope scope = getScope(clazz);
        if (scope != STATELESS && clazz.getTypeParameters().length > 0) {
            throw new CdiException(PARAMETERIZED_NON_STATELESS, clazz.getCanonicalName());
        }

        ClassInfo ci = new ClassInfo(clazz);

        for (Field field : ci.x.declaredFields) {
            if (injectable(field)) {
                validateFieldForInjection(ci, field);
            }
        }
        validateConstructor(ci);

        // generation
        String inheritorClassName = clazz.getName() + CDI_SUFFIX + contextId;
        String instantiatorClassName = inheritorClassName + "$" + I_SUFFIX;

        ci.x.stClasses.add(_class(_named(instantiatorClassName), _extends(Object.class), _implements($Instantiator.class),
            _withoutFields(), _withMethods(
                _method(_named("<init>"), _withoutParameterTypes(), _returnsNothing(), _withBody(
                    _statement(_invokeSpecialMethod(_ofClass(Object.class), _named("<init>"),
                        _withoutParameterTypes(), _returnsNothing(),
                        _on(_this), _withoutParameters()
                    ))
                )),
                _method(_named("$create"), _withParameterTypes($Context.class, $Descriptor[].class), _returns($CdiObject.class), _withBody(
                    _return(_invokeSpecialMethod(_ofClass(inheritorClassName), _named("<init>"),
                        _withParameterTypes($Context.class, $Descriptor[].class), _returnsNothing(),
                        _on(_new(inheritorClassName)),
                        _withParameters(_dup, _methodParam(0), _methodParam(1))
                    ))
                ))
            )
        ));

        // methods
        List<StMethod> methods = new ArrayList<>();

        methods.add(_method(_named("$construct"), _withoutParameterTypes(), _returnsNothing(),
            _withBody(getConstructorBody(ci))
        ));

        concat(stream(ci.x.methods), stream(ci.x.declaredMethods)).distinct().forEach(method -> {
            if (isAbstract(method.getModifiers())) {
                methods.addAll(implement(ci, method));
            }
        });
        // assume that validation is already done
        for (Method method : ci.x.declaredMethods) {
            if (isAbstract(method.getModifiers())) continue;
            Class<?>[] methodParameterTypes = method.getParameterTypes();
            int methodParameterCount = methodParameterTypes.length;

            List<StExpression> params = new ArrayList<>(methodParameterCount + 1);
            params.add(_this);

            List<Class<?>> paramTypes = new ArrayList<>(methodParameterCount + 1);
            paramTypes.add(null);

            for (int index = 0; index < methodParameterCount; index++) {
                params.add(_methodParam(index));
                paramTypes.add(methodParameterTypes[index]);
            }
            CollectionUtil.addIfNotNull(methods, generateTransformedMethod(ci, method,
                _invokeDynamic(method.getName(), _withParameterTypes(_types(paramTypes)), _returns(method.getReturnType()),
                    "override", CdiMetafactory.class, _withParameters(_expressions(params))
                )
            ));
        }

        methods.add(_staticMethod(_named("<clinit>"), _withoutParameterTypes(), _returnsNothing(), _withBody(
            _assignStatic(_named("$i"), _ofClass(inheritorClassName), _withType($Instantiator.class), _value(
                _invokeSpecialMethod(_ofClass(instantiatorClassName), _named("<init>"),
                    _withoutParameterTypes(), _returnsNothing(),
                    _on(_new(instantiatorClassName)),
                    _withParameters(_dup)
                )
            ))
        )));

        methods.add(_method(_named("<init>"), _withParameterTypes($Context.class, $Descriptor[].class), _returnsNothing(), _withBody(
            _statement(_invokeSpecialMethod(_ofClass(clazz), _named("<init>"),
                _withoutParameterTypes(), _returnsNothing(),
                _on(_this), _withoutParameters()
            )),
            !ci.x.hasContext ? _noop()
                : _assignMyField(_named("$c"), _withType($Context.class), _value(_methodParam(0))),
            !ci.x.hasDescriptors ? _noop()
                : _assignMyField(_named("$d"), _withType($Descriptor[].class), _value(_methodParam(1)))
        )));

        // fields
        List<StField> fields = new ArrayList<>();
        fields.add(_staticField(_named("$i"), _withType($Instantiator.class)));
        if (ci.x.hasContext) {
            fields.add(_field(_named("$c"), _withType($Context.class)));
        }
        if (ci.x.hasDescriptors) {
            fields.add(_field(_named("$d"), _withType($Descriptor[].class)));
        }

        ci.x.stClasses.add(_class(_named(inheritorClassName), _extends(clazz), _implements($CdiObject.class),
            _withFields(_fields(fields)), _withMethods(_methods(methods))
        ));

        return ci;
    }

    private StMethod generateTransformedMethod(ClassInfo ci, Method method, StExpression methodBody) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        List<StStatement> statements = new ArrayList<>();
        boolean override = false;
        for (int i = 0, parametersCount = parameterAnnotations.length; i < parametersCount; i++) {
            for (Annotation parameterAnnotation : parameterAnnotations[i]) {
                if (parameterAnnotation.annotationType() == Inject.class) {
                    statements.add(_assignMethodParam(i, getLookupExpression(ci, genericParameterTypes[i])));
                    override = true;
                }
                if (!override && context.valueTransformerRegistered(parameterAnnotation.annotationType())) {
                    override = true;
                }
            }
        }
        if (!override) {
            for (Annotation methodAnnotation : method.getAnnotations()) {
                if (context.valueTransformerRegistered(methodAnnotation.annotationType())) {
                    override = true;
                    break;
                } else if (context.methodTransformerRegistered(methodAnnotation.annotationType())) {
                    override = true;
                    break;
                }
            }
        }
        if (override) {
            StStatement methodStatement;
            if (method.getReturnType() == void.class) {
                methodStatement = _statement(methodBody);
            } else {
                methodStatement = _return(methodBody);
            }
            statements.add(methodStatement);
            return _overrideMethod(method, _withBody(_statements(statements)));
        }
        return null;
    }

    private List<StMethod> implement(ClassInfo ci, Method method) {
        String generatedName = method.getName() + ci.newMethodName();

        Type genericReturnType = method.getGenericReturnType();
        boolean voidReturnType = (genericReturnType == void.class);
        if (!voidReturnType) {
            validateLookup(ci, genericReturnType);
        }
        StExpression body = getLookupExpression(ci, genericReturnType);

        Class<?>[] methodParameterTypes = method.getParameterTypes();
        int methodParameterCount = methodParameterTypes.length;

        List<StExpression> params = new ArrayList<>(methodParameterCount + 1);
        params.add(_this);

        List<Class<?>> paramTypes = new ArrayList<>(methodParameterCount + 1);
        paramTypes.add(null);

        for (int index = 0; index < methodParameterCount; index++) {
            params.add(_methodParam(index));
            paramTypes.add(methodParameterTypes[index]);
        }

        StExpression invokeDynamic = _invokeDynamic(method.getName(), _withParameterTypes(_types(paramTypes)), _returns(method.getReturnType()),
            "implement", CdiMetafactory.class, _withParameters(_expressions(params)), generatedName
        );
        StMethod transformedMethod = generateTransformedMethod(ci, method, invokeDynamic);
        if (transformedMethod != null) {
            paramTypes = paramTypes.subList(1, methodParameterCount + 1);
            StMethod generatedMethod = _method(_named(generatedName),
                _withParameterTypes(_types(paramTypes)), _returns(method.getReturnType()),
                _withBody(voidReturnType ? _noop() : _return(body))
            );
            return asList(generatedMethod, transformedMethod);
        }
        return singletonList(_overrideMethod(method, _withBody(voidReturnType ? _noop() : _return(body))));
    }

    private static StStatement[] getConstructorBody(ClassInfo ci) {
        List<StStatement> statements = new ArrayList<>();
        for (Field field : ci.x.declaredFields) {
            if (injectable(field)) {
                statements.add(_assign(field, _of(_this), _value(getLookupExpression(ci, field.getGenericType()))));
            }
        }
        for (Method constructor : ci.x.constructors) {
            int parameterCount = constructor.getParameterCount();

            Type[] parameterTypes = constructor.getGenericParameterTypes();
            StExpression[] params = new StExpression[parameterCount];
            for (int i = 0; i < parameterCount; i++) {
                params[i] = getLookupExpression(ci, parameterTypes[i]);
            }

            statements.add(_statement(_invokeVirtualMethod(constructor, _on(_this), _withParameters(params))));
        }

        return _statements(statements);
    }

    private static StExpression getLookupExpression(ClassInfo ci, Type type) {
        if (type == Context.class) {
            ci.x.hasContext = true;
            return _myField(_named("$c"));
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType == Class.class) {
                Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
                TypeVariable actualTypeVariable = (TypeVariable) actualTypeArgument;

                return _getField(_ofClass($Descriptor.class), _named("c"), _withType(Class.class),
                                 _of(getTypeVariableDescriptor(ci, actualTypeVariable))
                );
            }
        }

        ci.x.hasContext = true;
        Class<?> rawClass = null;
        if (type instanceof Class) {
            rawClass = (Class) type;
        } else if (type instanceof ParameterizedType) {
            rawClass = (Class) ((ParameterizedType) type).getRawType();
        }

        StExpression context = _myField(_named("$c"));
        Method method;
        StExpression param;
        if (rawClass == null) {
            method = $lookup;
            param = getTypeVariableDescriptor(ci, (TypeVariable) type);
            rawClass = Object.class;
        } else {
            Scope scope = getScope(rawClass);
            if (scope == null) {
                method = $unscoped;
                param = _class(rawClass);
            } else {
                switch (scope) {
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

        return _cast(_toClass(rawClass), _invokeInterfaceMethod(method, _on(context), _withParameters(param)));
    }

    private static StExpression getTypeVariableDescriptor(ClassInfo ci, TypeVariable type) {
        return _arrayElement(_of(_myField(_named("$d"))), _withIndex(ci.paramIndex(type.getTypeName())));
    }

    private static StExpression getDescriptor(ClassInfo ci, Type type) {
        if (type instanceof TypeVariable) {
            return getTypeVariableDescriptor(ci, (TypeVariable) type);
        } else {
            List<StExpression> params = new ArrayList<>(2);
            Method method;
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class<?> clazz = (Class) parameterizedType.getRawType();

                params.add(_class(clazz));
                if (clazz.isAnnotationPresent(Scoped.class)) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length == 0) {
                        method = $0;
                    } else {
                        boolean optimize = true;
                        for (int i = 0, length = actualTypeArguments.length; i < length; i++) {
                            Type actualTypeArgument = actualTypeArguments[i];
                            boolean matches = (actualTypeArgument instanceof TypeVariable)
                                    && (i == ci.paramIndex(actualTypeArgument.getTypeName()));
                            if (!matches) {
                                optimize = false;
                                break;
                            }
                        }

                        if (optimize) {
                            params.add(_myField(_named("$d")));
                        } else {
                            StExpression[] elements = new StExpression[actualTypeArguments.length];
                            for (int i = 0, length = actualTypeArguments.length; i < length; i++) {
                                elements[i] = getDescriptor(ci, actualTypeArguments[i]);
                            }
                            params.add(_array($Descriptor.class, elements));
                        }
                        method = $;
                    }
                } else {
                    method = $0;
                }
            } else if (type instanceof Class) {
                params.add(_class((Class) type));
                method = $0;
            } else {
                throw new ImpossibleError();
            }
            return _invokeStaticMethod(method, _expressions(params));
        }
    }

    private void compileWithDependencies(ClassInfo ci, Set<ClassInfo> dejaVu) {
        if (ci.compiledClass != null) return;
        if (dejaVu.contains(ci)) return;

        dejaVu.add(ci);
        for (Class<?> clazz : ci.x.dependencies) {
            ClassInfo info = registry.computeIfAbsent(clazz, this::getClassInfo);
            compileWithDependencies(info, dejaVu);
        }
        dejaVu.remove(ci);

        Class<?> compiledClass = null;
        for (StClass stClass : ci.x.stClasses) {
            compiledClass = defineClass(compile(stClass));
        }
        ci.compiledClass = compiledClass;

        ci.cleanup();
    }

    private static void validateConstructor(ClassInfo ci) {
        for (Method method : ci.x.declaredMethods) {
            if (method.isAnnotationPresent(Inject.class)) {
                ci.x.constructors.add(method);
            }
        }
        for (Method constructor : ci.x.constructors) {
            validateConstructor(ci, constructor);
            for (Type parameterType : constructor.getGenericParameterTypes()) {
                validateLookup(ci, parameterType);
            }
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
        Class<?>[] exceptions = constructor.getExceptionTypes();
        for (Class<?> exceptionClass : exceptions) {
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
            Class<?> clazz = (Class) type; // raw type should be validated in future
            if (clazz.isAnnotationPresent(Scoped.class)) {
                TypeVariable[] params = clazz.getTypeParameters();
                if (params.length > 0) {
                    throw new CdiException(GENERIC_PARAMETERS_COUNT_MISMATCH, clazz.getCanonicalName(), 0, params.length);
                }
                ci.x.dependencies.add(clazz);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Class<?> clazz = (Class) pType.getRawType(); // raw type should be validated in future, but not parameterized type. That would cause infinite recursion in some cases
            if (clazz.isAnnotationPresent(Scoped.class)) {
                for (Type param : pType.getActualTypeArguments()) {
                    validateType(ci, param);
                }
                ci.x.dependencies.add(clazz);
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
        public Class<?> compiledClass = null;
        public Temp x;

        public static class Temp {
            public final Class<?> clazz;
            public final String originalName;
            public final Field[] declaredFields;
            public final Method[] declaredMethods;
            public final Method[] methods;
            public final Map<String, Integer> paramsIndex = new HashMap<>();
            public final Set<Class> dependencies = new HashSet<>();
            public final List<Method> constructors = new ArrayList<>();
            public final List<StClass> stClasses = new ArrayList<>();

            public boolean hasContext = false;
            public boolean hasDescriptors = false;
            public int counter = 0;

            public Temp(Class<?> clazz) {
                this.clazz = clazz;
                this.originalName = clazz.getCanonicalName();
                this.declaredFields = clazz.getDeclaredFields();
                this.declaredMethods = clazz.getDeclaredMethods();
                this.methods = clazz.getMethods();
            }
        }

        public ClassInfo(Class<?> clazz) {
            this.x = new Temp(clazz);
            TypeVariable[] params = clazz.getTypeParameters();
            for (int i = 0; i < params.length; i++) {
                this.x.paramsIndex.put(params[i].getName(), i);
            }
        }

        public String newMethodName() {
            return "$generated" + x.counter++;
        }

        public int paramIndex(String typeName) {
            return x.paramsIndex.get(typeName);
        }

        public void cleanup() {
            x = null;
        }
    }
}
