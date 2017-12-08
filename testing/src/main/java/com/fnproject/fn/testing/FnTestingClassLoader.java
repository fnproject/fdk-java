package com.fnproject.fn.testing;

import com.fnproject.fn.runtime.EntryPoint;
import com.fnproject.fn.runtime.flow.CompleterClient;
import com.fnproject.fn.runtime.flow.CompleterClientFactory;
import com.fnproject.fn.runtime.flow.FlowRuntimeGlobals;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Testing classloader that loads all classes afresh when needed, otherwise delegates shared classes to the parent classloader
 */
class FnTestingClassLoader extends ClassLoader {
    private final List<String> sharedPrefixes;
    private final Map<String, Class<?>> loaded = new HashMap<>();

    FnTestingClassLoader(ClassLoader parent, List<String> sharedPrefixes) {
        super(parent);
        this.sharedPrefixes = sharedPrefixes;
    }

    boolean isShared(String classOrPackageName) {
        for (String prefix : sharedPrefixes) {
            if (("=" + classOrPackageName).equals(prefix) || classOrPackageName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized Class<?> loadClass(String className) throws ClassNotFoundException {
        Class<?> definedClass = loaded.get(className);
        if (definedClass != null) {
            return definedClass;
        }

        Class<?> cls = null;
        if (isShared(className)) {
            cls = getParent().loadClass(className);
        }

        if (cls == null) {
            try {
                InputStream in = getResourceAsStream(className.replace('.', '/') + ".class");
                if (in == null) {
                    throw new ClassNotFoundException("Class not found :" + className);
                }

                byte[] clsBytes = IOUtils.toByteArray(in);
                cls = defineClass(className, clsBytes, 0, clsBytes.length);
                resolveClass(cls);

            } catch (IOException e) {
                throw new ClassNotFoundException(className, e);
            }
        }
        loaded.put(className, cls);
        return cls;
    }

    void setCompleterClient(CompleterClientFactory completerClientFactory) {
        try {
            Class<?> completerGlobals = loadClass(FlowRuntimeGlobals.class.getName());
            callMethodInFnClassloader(completerGlobals, "setCompleterClientFactory", CompleterClientFactory.class).invoke(completerGlobals, completerClientFactory);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException | IllegalArgumentException e) {
            throw new RuntimeException("Something broke in the reflective classloader", e);
        }
    }

    public int run(Map<String, String> mutableEnv, InputStream is, PrintStream functionOut, PrintStream functionErr, String... s) {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {


            Thread.currentThread().setContextClassLoader(this);

            Class<?> entryPoint_class = loadClass(EntryPoint.class.getName());
            Object entryPoint = entryPoint_class.newInstance();

            return (int) callMethodInFnClassloader(entryPoint, "run", Map.class, InputStream.class, OutputStream.class, PrintStream.class, String[].class)
                    .invoke(entryPoint, mutableEnv, is, functionOut, functionErr, s);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException e) {
            throw new RuntimeException("Something broke in the reflective classloader", e);
        }finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    private Method callMethodInFnClassloader(Object target, String method, Class... types) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> targetClass;
        if (target instanceof Class) {
            targetClass = (Class) target;
        } else {
            targetClass = target.getClass();
        }
        return targetClass.getMethod(method, types);
    }
}