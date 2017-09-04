package com.fnproject.fn.runtime.cloudthreads;


import com.fnproject.fn.api.Headers;
import com.fnproject.fn.api.cloudthreads.*;
import com.fnproject.fn.testing.FnTestingRule;
import com.fnproject.fn.testing.PlatformError;
import com.fnproject.fn.testing.cloudthreads.Datum;
import com.fnproject.fn.testing.cloudthreads.ForkingClassLoader;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created on 26/08/2017.
 * <p>
 * (c) 2017 Oracle Corporation
 */
public class TestSupport {
    public static CompletionId completionId(String id) {
        return new CompletionId(id);
    }

    public static String completionIdString(CompletionId id) {
        return id.getId();
    }

    public static ThreadId threadId(String id) {
        return new ThreadId(id);
    }

    public static HttpMethod httpMethod(String name) { return HttpMethod.valueOf(name); }

    public static Headers headers(Map<String, String> hs) { return Headers.fromMap(hs); }

    private static Object completer;
    private static PrintStream logs;

    private static CompleterClient makeCompleterClient() {
        return (CompleterClient) Proxy.newProxyInstance(CompleterClient.class.getClassLoader(),
                new Class[]{CompleterClient.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        // Map classes of arguments to alien class

                        logs.println("Invoking " + method.getName());
                        Class[] pts = method.getParameterTypes();
                        Object[] alienArgs = new Object[args.length];
                        for (int i = 0; i < pts.length; i++) {
                            if (pts[i] == ThreadId.class) {
                                pts[i] = FnTestingRule.class.getClassLoader().loadClass(ThreadId.class.getName());
                                alienArgs[i] = alienateThreadId((ThreadId) args[i]);
                            } else if (pts[i] == CompletionId.class) {
                                pts[i] = FnTestingRule.class.getClassLoader().loadClass(CompletionId.class.getName());
                                alienArgs[i] = alienateCompletionId((CompletionId) args[i]);
                            } else if (pts[i] == HttpMethod.class) {
                                pts[i] = FnTestingRule.class.getClassLoader().loadClass(HttpMethod.class.getName());
                                alienArgs[i] = alienateHttpMethod((HttpMethod) args[i]);
                            } else if (pts[i] == Headers.class) {
                                pts[i] = FnTestingRule.class.getClassLoader().loadClass(Headers.class.getName());
                                alienArgs[i] = alienateHeaders((Headers) args[i]);
                            } else if (pts[i] == List.class) {
                                // It's a list of completion Ids
                                alienArgs[i] = ((List<CompletionId>) args[i]).stream().map((cid) -> alienateCompletionId(cid)).collect(Collectors.toList());
                            } else {
                                alienArgs[i] = args[i];
                            }
                        }

                        // Locate method on proxy
                        Method alienMethod = null;
                        try {
                            alienMethod = completer.getClass().getDeclaredMethod(method.getName(), pts);
                        } catch (NoSuchMethodException e) {
                            logs.println("No such method " + method.getName() + " with parameter types " + Arrays.toString(pts));
                            throw e;
                        }
                        // Invoke
                        Object result = null;
                        try {
                            result = alienMethod.invoke(completer, alienArgs);
                        } catch (IllegalArgumentException e) {
                            logs.println("Illegal argument during cross-call: " + e);
                            throw e;
                        } catch (InvocationTargetException ite) {
                            // We need to naturalise the exception and re-throw
                            logs.println("Caught error: " + ite);
                            throw (Throwable) naturaliseObject(ite.getCause());
                        }
                        // Cast back
                        return naturaliseObject(result);
/*                        if (method.getReturnType() == ThreadId.class) {
                            return naturaliseThreadId(result);
                        } else if (method.getReturnType() == CompletionId.class) {
                            return naturaliseCompletionId(result);
                        } else {
                            return result;
                        }*/
                    }

                    private Object alienateThreadId(ThreadId tid) {
                        try {
                            Class<?> testSupport = FnTestingRule.class.getClassLoader().loadClass(TestSupport.class.getName());
                            return testSupport.getMethod("threadId", String.class).invoke(null, tid.getId());
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    private Object alienateCompletionId(CompletionId cid) {
                        try {
                            Class<?> testSupport = FnTestingRule.class.getClassLoader().loadClass(TestSupport.class.getName());
                            return testSupport.getMethod("completionId", String.class).invoke(null, cid.getId());
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    private Object alienateHttpMethod(HttpMethod hm) {
                        try {
                            Class<?> testSupport = FnTestingRule.class.getClassLoader().loadClass(TestSupport.class.getName());
                            return testSupport.getDeclaredMethod("httpMethod", String.class).invoke(null, hm.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    private Object alienateHeaders(Headers headers) {
                        try {
                            Class<?> testSupport = FnTestingRule.class.getClassLoader().loadClass(TestSupport.class.getName());
                            return testSupport.getDeclaredMethod("headers", Map.class).invoke(null, headers.getAll());
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    private ThreadId naturaliseThreadId(Object tid) {
                        try {
                            Class<?> cl = tid.getClass();
                            Method m = cl.getDeclaredMethod("getId");
                            String id = (String) m.invoke(tid);
                            return TestSupport.threadId(id);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    private CompletionId naturaliseCompletionId(Object cid) {
                        try {
                            Class<?> cl = cid.getClass();
                            Method m = cl.getDeclaredMethod("getId");
                            m.setAccessible(true);
                            String id = (String) m.invoke(cid);
                            return TestSupport.completionId(id);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    private Headers naturaliseHeaders(Object hs) {
                        try {
                            Class<?> cl = hs.getClass();
                            Method m = cl.getDeclaredMethod("getAll");
                            Map<String, String> map = (Map<String, String>) m.invoke(hs);
                            return TestSupport.headers(map);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    private HttpResponse naturaliseHttpResponse(Object resp) {
                        try {
                            Class<?> cl = resp.getClass();
                            Method msc = cl.getMethod("getStatusCode");
                            Method mhs = cl.getMethod("getHeaders");
                            Method mbab = cl.getMethod("getBodyAsBytes");
                            msc.setAccessible(true);
                            mhs.setAccessible(true);
                            mbab.setAccessible(true);
                            final int sc = (int) msc.invoke(resp);
                            final Headers hs = naturaliseHeaders(mhs.invoke(resp));
                            final byte[] body = (byte[]) mbab.invoke(resp);
                            return new HttpResponse() {
                                @Override
                                public int getStatusCode() { return sc; }

                                @Override
                                public Headers getHeaders() { return hs; }

                                @Override
                                public byte[] getBodyAsBytes() { return body; }
                            };
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    private HttpMethod naturaliseHttpMethod(Object mth) {
                        try {
                            Class<?> cl = mth.getClass();
                            Method msc = cl.getMethod("toString");
                            return HttpMethod.valueOf((String) msc.invoke(mth));
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    private HttpRequest naturaliseHttpRequest(Object resp) {
                        try {
                            Class<?> cl = resp.getClass();
                            Method msc = cl.getMethod("getMethod");
                            Method mhs = cl.getMethod("getHeaders");
                            Method mbab = cl.getMethod("getBodyAsBytes");
                            msc.setAccessible(true);
                            mhs.setAccessible(true);
                            mbab.setAccessible(true);
                            final HttpMethod mth = naturaliseHttpMethod(msc.invoke(resp));
                            final Headers hs = naturaliseHeaders(mhs.invoke(resp));
                            final byte[] body = (byte[]) mbab.invoke(resp);
                            return new HttpRequest() {
                                @Override
                                public HttpMethod getMethod() { return mth; }

                                @Override
                                public Headers getHeaders() { return hs; }

                                @Override
                                public byte[] getBodyAsBytes() { return body; }
                            };
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    private CompleterClient.ExternalCompletion naturaliseExternalCompletion(Object ec) {
                        try {
                            Class<?> cl = ec.getClass();
                            Method mcid = cl.getMethod("completionId");
                            Method mc = cl.getMethod("completeURI");
                            Method mf = cl.getMethod("failureURI");
                            mcid.setAccessible(true);
                            mc.setAccessible(true);
                            mf.setAccessible(true);
                            final CompletionId cid = naturaliseCompletionId(mcid.invoke(ec));
                            final URI compUri = (URI) mc.invoke(ec);
                            final URI failUri = (URI) mf.invoke(ec);
                            return new CompleterClient.ExternalCompletion() {
                                @Override
                                public CompletionId completionId() { return cid; }

                                @Override
                                public URI completeURI() { return compUri; }

                                @Override
                                public URI failureURI() { return failUri; }
                            };
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }

                    private Object naturaliseObject(Object t) {
                        if (t == null) {
                            return null;
                        }

                        try {
                            if (FnTestingRule.class.getClassLoader().loadClass(HttpRequest.class.getName()).isAssignableFrom(t.getClass())) {
                                return naturaliseHttpRequest(t);
                            } else if (FnTestingRule.class.getClassLoader().loadClass(HttpResponse.class.getName()).isAssignableFrom(t.getClass())) {
                                return naturaliseHttpResponse(t);
                            } else if (FnTestingRule.class.getClassLoader().loadClass(CompleterClient.ExternalCompletion.class.getName()).isAssignableFrom(t.getClass())) {
                                return naturaliseExternalCompletion(t);
                            } else if (t.getClass() == FnTestingRule.class.getClassLoader().loadClass(CloudCompletionException.class.getName())) {
                                return new CloudCompletionException(((Throwable) t).getMessage(), (Throwable) naturaliseObject(((Throwable) t).getCause()));
                            } else if (t.getClass() == FnTestingRule.class.getClassLoader().loadClass(FunctionInvocationException.class.getName())) {
                                Method m = t.getClass().getDeclaredMethod("getFunctionResponse");
                                HttpResponse resp = naturaliseHttpResponse(m.invoke(t));
                                return new FunctionInvocationException(resp);
                            } else if (t.getClass() == FnTestingRule.class.getClassLoader().loadClass(ExternalCompletionException.class.getName())) {
                                Method m = t.getClass().getDeclaredMethod("getExternalRequest");
                                HttpRequest req = naturaliseHttpRequest(m.invoke(t));
                                return new ExternalCompletionException(req);
                            }
                        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            throw new IllegalStateException("Cannot locate class during exception mapping", e);
                        }

                        try {
                            // Serialise and deserialise these to get the replaced stack
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = new ObjectOutputStream(bos);
                            oos.writeObject(t);
                            oos.flush();

                            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                            ObjectInputStream ois = new ObjectInputStream(bis);

                            return ois.readObject();

                        } catch (IOException e) {
                            throw new IllegalStateException("Cannot serialize class during object mapping", e);
                        } catch (ClassNotFoundException e) {
                            throw new IllegalStateException("Cannot locate class during object mapping", e);
                        }

                    }
                });

    }

    public static synchronized void installCompleterClientFactory(Object completer, PrintStream logs) {
        TestSupport.completer = completer;
        TestSupport.logs = logs;

        CloudThreadsContinuationInvoker.setCompleterClientFactory((CompleterClientFactory) () -> makeCompleterClient());
    }
}


