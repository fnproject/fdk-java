package com.fnproject.fn.runtime;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.runtime.exception.FunctionConfigurationException;
import com.fnproject.fn.runtime.exception.InvalidFunctionDefinitionException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionLoader {
    private static ClassLoader contextClassLoader = FunctionLoader.class.getClassLoader();

    /**
     * create a function runtime context for a given class and method name
     *
     * @param className the class name to load
     * @param fnName    the function in the class
     * @return a new runtime context
     */
    public MethodWrapper loadClass(String className, String fnName) {
        Class<?> targetClass = loadClass(className);
        return new DefaultMethodWrapper(targetClass, getTargetMethod(targetClass, fnName));
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

    // MJG delete this?
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

// MJG delete this?
    private boolean isConfigurationMethod(Method m) {
        return m.getDeclaredAnnotationsByType(FnConfiguration.class).length > 0 && !m.getDeclaringClass().isInterface();
    }


    private List<Method> findMethodsByName(String fnName, Class<?> fnClass) {
        return Arrays.stream(fnClass.getMethods())
                .filter((m) -> !m.getDeclaringClass().equals(Object.class))
                .filter(m -> fnName.equals(m.getName()))
                .filter(m -> !m.isBridge())
                .collect(Collectors.toList());
    }

    private Class<?> loadClass(String className) {
        Class<?> fnClass;
        try {
            fnClass = Class.forName(className);
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
     * @param loader
     */
    public static void setContextClassLoader(ClassLoader loader) {
        contextClassLoader = loader;
    }
}
