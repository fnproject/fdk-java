package com.fnproject.fn.runtime;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.MethodWrapper;
import com.fnproject.fn.api.exception.FunctionConfigurationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

/**
 * Loads function entry points based on their class name and method name creating a {@link FunctionRuntimeContext}
 */
public class FunctionConfigurer {


    /**
     * create a function runtime context for a given class and method name
     *
     * @param runtimeContext    The runtime context encapsulating the function to be run
     */
    public void configure(FunctionRuntimeContext runtimeContext) {
        validateConfigurationMethods(runtimeContext.getMethodWrapper());
        applyUserConfigurationMethod(runtimeContext.getMethodWrapper(), runtimeContext);
    }


    private void validateConfigurationMethods(MethodWrapper function) {
        // Function configuration methods must have a void return type.
        Arrays.stream(function.getTargetClass().getMethods())
                .filter(this::isConfigurationMethod)
                .filter((m) -> !m.getReturnType().equals(Void.TYPE))
                .forEach((m) -> {
                    throw new FunctionConfigurationException("Configuration method '" +
                            m.getName() +
                            "' does not have a void return type");
                });

        // If target method is static, configuration methods cannot be non-static.
        if (Modifier.isStatic(function.getTargetMethod().getModifiers())) {
            Arrays.stream(function.getTargetClass().getMethods())
                    .filter(this::isConfigurationMethod)
                    .filter((m) -> !(Modifier.isStatic(m.getModifiers())))
                    .forEach((m) -> {
                        throw new FunctionConfigurationException("Configuration method '" +
                                m.getName() +
                                "' cannot be an instance method if the function method is a static method");
                    });
        }
    }

    private void applyUserConfigurationMethod(MethodWrapper targetClass, FunctionRuntimeContext runtimeContext) {

        Arrays.stream(targetClass.getTargetClass().getMethods())
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


}
