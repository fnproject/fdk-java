package com.fnproject.fn.testing.cloudthreads;

import com.fnproject.fn.runtime.EntryPoint;
import com.fnproject.fn.runtime.cloudthreads.CloudThreadRuntimeGlobals;
import com.fnproject.fn.runtime.cloudthreads.CompleterClient;
import com.fnproject.fn.runtime.cloudthreads.CompleterClientFactory;
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
public class FnTestingClassLoader extends ClassLoader {
    private final List<String> sharedPrefixes;
    private final Map<String, Class<?>> loaded = new HashMap<>();

    public FnTestingClassLoader(ClassLoader parent, List<String> sharedPrefixes) {
        super(parent);
        this.sharedPrefixes = sharedPrefixes;
    }

    @Override
    public synchronized Class<?> loadClass(String s) throws ClassNotFoundException {
        Class<?> definedClass = loaded.get(s);
        if (definedClass != null) {
            return definedClass;
        }

        Class<?> cls = null;
        for (String prefix : sharedPrefixes) {
            if (("=" + s).equals(prefix) || s.startsWith(prefix)) {
                cls = getParent().loadClass(s);
                break;
            }
        }


        if (cls == null) {
            try {
                InputStream in = getResourceAsStream(s.replace('.', '/') + ".class");
                if (in == null){
                    throw new ClassNotFoundException("Class not found :" + s);
                }

                byte[] clsBytes = IOUtils.toByteArray(in);
                cls = defineClass(s, clsBytes, 0, clsBytes.length);
                resolveClass(cls);

            } catch (IOException e) {
                throw new ClassNotFoundException(s, e);
            }
        }
        loaded.put(s, cls);
        return cls;
    }

    /**
     * @param completer
     */
    public void setCompleterClient(CompleterClient completer) {
        try {
            Class<?> completerGlobals = loadClass(CloudThreadRuntimeGlobals.class.getName());
            CompleterClientFactory ccf = (CompleterClientFactory) () -> completer;
            method(completerGlobals, "setCompleterClientFactory", CompleterClientFactory.class).invoke(completerGlobals, ccf);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException | IllegalArgumentException e) {
            throw new RuntimeException("Something broke in the reflective classloader", e);
        }
    }

    public int run(Map<String, String> mutableEnv, InputStream is, PrintStream functionOut, PrintStream functionErr, String... s) {
        try {

            Class<?> entryPoint_class = loadClass(EntryPoint.class.getName());
            Object entryPoint = entryPoint_class.newInstance();

            return (int) method(entryPoint, "run", Map.class, InputStream.class, OutputStream.class, PrintStream.class, String[].class)
                    .invoke(entryPoint, mutableEnv, is, functionOut, functionErr, s);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException e) {
            throw new RuntimeException("Something broke in the reflective classloader", e);
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