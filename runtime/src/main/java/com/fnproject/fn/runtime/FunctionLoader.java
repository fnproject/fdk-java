package com.fnproject.fn.runtime;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.runtime.exception.FunctionConfigurationException;
import com.fnproject.fn.runtime.exception.InvalidFunctionDefinitionException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads function entry points based on their class name and method name creating a {@link FunctionRuntimeContext}
 */
public class FunctionLoader {


    /**
     * create a function runtime context for a given class and method name
     *
     * @param className the class name to load
     * @param fnName    the function in the class
     * @return a new runtime context
     */
    public FunctionRuntimeContext loadFunction(String className, String fnName, Map<String, String> config) {
        Class<?> targetClass = loadClass(className);

        List<Method> namedMethods = findMethodsByName(fnName, targetClass);

        if (namedMethods.isEmpty()) {
            String names = Arrays.stream(targetClass.getDeclaredMethods())
                    .filter((m) -> !m.getDeclaringClass().equals(Object.class))
                    .map(Method::getName)
                    .filter((name) -> !name.startsWith("$"))
                    .reduce((x, y) -> (x + "," + y)).orElseGet(() -> "");

            throw new InvalidFunctionDefinitionException("Method '" + fnName + "' was not found " +
                    "in class '" + className + "'. Available functions were: [" + names + "]");
        }

        if (namedMethods.size() > 1) {
            throw new InvalidFunctionDefinitionException("Multiple methods match name " + fnName + " in " + className + "  matching methods were [" + namedMethods.stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        Method targetMethod = namedMethods.get(0);

        validateLoadedFunctionClass(targetClass, targetMethod);

        FunctionRuntimeContext runtimeContext = new FunctionRuntimeContext(targetClass, targetMethod, config);

        applyUserConfigurationMethod(targetClass, runtimeContext);

        return runtimeContext;
    }

    private void validateLoadedFunctionClass(Class<?> targetClass, Method targetMethod) {
        // Function configuration methods must have a void return type.
        Arrays.stream(targetClass.getMethods())
                .filter(this::isConfigurationMethod)
                .filter((m) -> !m.getReturnType().equals(Void.TYPE))
                .forEach((m) -> {
                    throw new FunctionConfigurationException("Configuration method '" +
                            m.getName() +
                            "' does not have a void return type");
                });

        // If target method is static, configuration methods cannot be non-static.
        if (Modifier.isStatic(targetMethod.getModifiers())) {
            Arrays.stream(targetClass.getMethods())
                    .filter(this::isConfigurationMethod)
                    .filter((m) -> !(Modifier.isStatic(m.getModifiers())))
                    .forEach((m) -> {
                        throw new FunctionConfigurationException("Configuration method '" +
                                m.getName() +
                                "' cannot be an instance method if the function method is a static method");
                    });
        }
    }

    private void applyUserConfigurationMethod(Class<?> targetClass, FunctionRuntimeContext runtimeContext) {

        Arrays.stream(targetClass.getMethods())
                .filter(this::isConfigurationMethod)
                .sorted(Comparator.<Method>comparingInt((m) -> Modifier.isStatic(m.getModifiers()) ? 0 : 1) // run static methods first
                        .thenComparing(Comparator.<Method>comparingInt((m) -> { // depth first in implementation
                            int depth = 0;
                            Class<?> cc = m.getDeclaringClass();
                            while (null != cc) {
                                depth++;
                                cc = cc.getSuperclass();
                            }
                            return depth;
                        })))
                .forEach(configMethod -> {
                    try {
                        Optional<Object> fnInstance = runtimeContext.getInvokeInstance();

                        // Allow the runtime context parameter to be optional
                        if (configMethod.getParameterCount() == 0) {
                            configMethod.invoke(fnInstance.orElse(null));
                        } else {
                            configMethod.invoke(fnInstance.orElse(null), runtimeContext);
                        }

                    } catch ( InvocationTargetException e){
                        throw new FunctionConfigurationException("Error invoking configuration method: " + configMethod.getName(), e.getCause());

                    } catch (IllegalAccessException e) {
                        throw new FunctionConfigurationException("Error invoking configuration method: " + configMethod.getName(), e);
                    }
                });
    }


    private boolean isConfigurationMethod(Method m) {
        return m.getDeclaredAnnotationsByType(FnConfiguration.class).length > 0 && !m.getDeclaringClass().isInterface();
    }


    private static ClassLoader contextClassLoader = MethodFunctionInvoker.class.getClassLoader();


    private List<Method> findMethodsByName(String fnName, Class<?> fnClass) {
        return Arrays.stream(fnClass.getMethods())
                .filter((m) -> !m.getDeclaringClass().equals(Object.class))
                .filter(m -> fnName.equals(m.getName()))
                .filter(m -> !m.isBridge())
                .collect(Collectors.toList());
    }


    /**
     * Override the classloader used for fn class resolution
     * Primarily for testing, otherwise the system/default  classloader is used.
     *
     * @param contextClassLoader
     */
    public static void setContextClassLoader(ClassLoader contextClassLoader) {
        FunctionLoader.contextClassLoader = contextClassLoader;
    }

    private Class<?> loadClass(String className) {
        Class<?> fnClass;
        try {
            fnClass = contextClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new InvalidFunctionDefinitionException(String.format("Class '%s' not found in function jar\n" +
                    "Its likely that the 'cmd' entry in func.yaml is incorrect.", className));
        }
        return fnClass;
    }
}
