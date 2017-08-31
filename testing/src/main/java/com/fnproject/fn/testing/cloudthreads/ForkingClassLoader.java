package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.api.cloudthreads.CloudThreadRuntime;
import com.fnproject.fn.api.cloudthreads.PlatformException;
import com.fnproject.fn.runtime.EntryPoint;
import com.fnproject.fn.runtime.cloudthreads.CloudThreadsContinuationInvoker;
import com.fnproject.fn.runtime.cloudthreads.CompleterClient;
import com.fnproject.fn.runtime.cloudthreads.CompleterClientFactory;
import com.fnproject.fn.runtime.cloudthreads.TestSupport;
import org.apache.commons.io.IOUtils;

import javax.management.ReflectionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForkingClassLoader extends ClassLoader {
    private final List<String> prefixes;
    private final Map<String, Class<?>> loaded = new HashMap<>();
    private final PrintStream originalSystemErr;

    private interface NotForked {}

    private static Class<?> UNFORKED_TYPE = NotForked.class;

    public ForkingClassLoader(java.lang.ClassLoader parent, List<String> prefixes, PrintStream originalSystemErr) {
        super(parent);
        this.prefixes = prefixes;
        this.originalSystemErr = originalSystemErr;
    }

    public Class getOriginalClass(Class c) {
        try {
            return super.loadClass(c.getName());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public synchronized Class<?> loadClass(String s) throws ClassNotFoundException {
        // originalSystemErr.println("Loading class " + s);
        Class<?> definedClass = loaded.get(s);
        if (definedClass == UNFORKED_TYPE) {
            // Delegate directly for these
            return getParent().loadClass(s);
        } else if (definedClass != null) {
            return definedClass;
        }

        for (String prefix : prefixes) {
            if (s.startsWith(prefix)) {
                try {
                    InputStream in = getResourceAsStream(s.replace('.', '/') + ".class");
                    assert in != null;
                    byte[] cls = IOUtils.toByteArray(in);
                    definedClass = defineClass(s, cls, 0, cls.length);
                    resolveClass(definedClass);
                    loaded.put(s, definedClass);
                    return definedClass;
                } catch (IOException e) {
                    throw new ClassNotFoundException(s, e);
                }
            }
        }
        loaded.put(s, UNFORKED_TYPE);
        return getParent().loadClass(s);
    }

    public void setCompleterClientFactory(CompleterClient completer) {
        try {
            originalSystemErr.println("Setting completer client factory");

            Class<?> testSupport = loadClass(TestSupport.class.getName());
            method(testSupport, "installCompleterClientFactory", Object.class, PrintStream.class).invoke(testSupport, completer, originalSystemErr);
            originalSystemErr.println("Set completer client factory correctly");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException | IllegalArgumentException e) {
            e.printStackTrace(originalSystemErr);
            throw new PlatformException("Something broke in the reflective classloader", e);
        }
    }

    public int run(Map<String, String> mutableEnv, InputStream is, PrintStream functionOut, PrintStream functionErr, String... s) {
        try {
            originalSystemErr.println("calling run()");

            Class<?> entryPoint_class = loadClass(EntryPoint.class.getName());
            Object entryPoint = entryPoint_class.newInstance();
            int result = (int) method(entryPoint, "run",Map.class, InputStream.class, OutputStream.class, PrintStream.class, String[].class)
                    .invoke(entryPoint, mutableEnv, is, functionOut, functionErr, s);

            originalSystemErr.println("returned " + result);
            return result;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException e) {
            e.printStackTrace(originalSystemErr);
            throw new PlatformException("Something broke in the reflective classloader", e);
        }
    }

    private Method method(Object target, String method, Class... types) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> targetClass;
        if (target instanceof Class) {
            targetClass = (Class) target;
        } else {
            targetClass = target.getClass();
        }
        return targetClass.getMethod(method, types);
    }
}