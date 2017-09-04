package com.fnproject.fn.runtime;

import com.fnproject.fn.runtime.exception.InvalidFunctionDefinitionException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FnFunction {
    private Class<?> targetClass;
    private Method targetMethod;
    private static ClassLoader contextClassLoader = MethodFunctionInvoker.class.getClassLoader();

    public FnFunction(Class<?> cls, Method method) {
        targetClass = cls;
        targetMethod = method;
    }

    public FnFunction(String className, String fnName) {
        targetClass = loadClass(className);
        targetMethod = getTargetMethod(targetClass, fnName);
    }

    private Method getTargetMethod(Class<?> targetClass, String method) {
        List<Method> namedMethods = findMethodsByName(method, targetClass);

        if (namedMethods.isEmpty()) {
            String names = Arrays.stream(targetClass.getDeclaredMethods())
                    .filter((m) -> !m.getDeclaringClass().equals(Object.class))
                    .map(Method::getName)
                    .filter((name) -> !name.startsWith("$"))
                    .reduce((x, y) -> (x + "," + y)).orElseGet(() -> "");

            throw new InvalidFunctionDefinitionException("Method '" + method + "' was not found " +
                    "in class '" + targetClass.getCanonicalName() + "'. Available functions were: [" + names + "]");
        }

        if (namedMethods.size() > 1) {
            throw new InvalidFunctionDefinitionException("Multiple methods match name " + method + " in " + targetClass.getCanonicalName() + "  matching methods were [" + namedMethods.stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        return namedMethods.get(0);
    }

    private List<Method> findMethodsByName(String fnName, Class<?> fnClass) {
        return Arrays.stream(fnClass.getMethods())
                .filter((m) -> !m.getDeclaringClass().equals(Object.class))
                .filter(m -> fnName.equals(m.getName()))
                .filter(m -> !m.isBridge())
                .collect(Collectors.toList());
    }


    public Class<?> getTargetClass() { return targetClass; }

    public Method getTargetMethod() { return targetMethod; }

    private Class<?> loadClass(String className) {
        Class<?> fnClass;
        try {
            fnClass = contextClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new InvalidFunctionDefinitionException(String.format("Class '%s' not found in function jar. " +
                    "It's likely that the 'cmd' entry in func.yaml is incorrect.", className));
        }
        return fnClass;
    }

    /**
     * Override the classloader used for fn class resolution
     * Primarily for testing, otherwise the system/default  classloader is used.
     *
     * @param contextClassLoader
     */
    public static void setContextClassLoader(ClassLoader contextClassLoader) {
        contextClassLoader = contextClassLoader;
    }
}
