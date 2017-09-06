package com.fnproject.fn.runtime;

import com.fnproject.fn.api.*;
import com.fnproject.fn.runtime.cloudthreads.CloudThreadsContinuationInvoker;
import com.fnproject.fn.runtime.coercion.*;
import com.fnproject.fn.runtime.coercion.jackson.JacksonCoercion;
import com.fnproject.fn.runtime.exception.FunctionClassInstantiationException;
import com.fnproject.fn.runtime.exception.FunctionConfigurationException;
import com.fnproject.fn.runtime.exception.FunctionInputHandlingException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class FunctionRuntimeContext implements RuntimeContext {

    private final Map<String, String> config;
    private final MethodWrapper method;
    private Map<String, Object> attributes = new HashMap<>();
    private List<FunctionInvoker> configuredInvokers = Arrays.asList(new CloudThreadsContinuationInvoker(), new MethodFunctionInvoker());

    private Object instance;

    private final List<InputCoercion> builtinInputCoercions = Arrays.asList(new StringCoercion(), new ByteArrayCoercion(), new InputEventCoercion(), JacksonCoercion.instance());
    private final List<InputCoercion> userInputCoercions = new LinkedList<>();
    private final List<OutputCoercion> builtinOutputCoercions = Arrays.asList(new StringCoercion(), new ByteArrayCoercion(), new VoidCoercion(), new OutputEventCoercion(), JacksonCoercion.instance());
    private final List<OutputCoercion> userOutputCoercions = new LinkedList<>();

    public FunctionRuntimeContext(MethodWrapper method, Map<String, String> config) {
        this.method = method;
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public Optional<Object> getInvokeInstance() {
        if (!Modifier.isStatic(getMethod().getTargetMethod().getModifiers())) {
            if (instance == null) {
                try {
                    Constructor<?> constructors[] = getMethod().getTargetClass().getConstructors();
                    if (constructors.length == 1) {
                        Constructor<?> ctor = constructors[0];
                        if (ctor.getParameterTypes().length == 0) {
                            instance = ctor.newInstance();
                        } else if (ctor.getParameterTypes().length == 1) {
                            if (RuntimeContext.class.isAssignableFrom(ctor.getParameterTypes()[0])) {
                                instance = ctor.newInstance(FunctionRuntimeContext.this);
                            } else {
                                if ( getMethod().getTargetClass().getEnclosingClass() != null && ! Modifier.isStatic(getMethod().getTargetClass().getModifiers()) ) {
                                    throw new FunctionClassInstantiationException("The function " + getMethod().getTargetClass() + " cannot be instantiated as it is a non-static inner class");
                                } else {
                                    throw new FunctionClassInstantiationException("The function " + getMethod().getTargetClass() + " cannot be instantiated as its constructor takes an unrecognized argument of type " + constructors[0].getParameterTypes()[0] + ". Function classes should have a single public constructor that takes either no arguments or a RuntimeContext argument");
                                }
                            }
                        } else {
                            throw new FunctionClassInstantiationException("The function " + getMethod().getTargetClass() + " cannot be instantiated as its constructor takes more than one argument. Function classes should have a single public constructor that takes either no arguments or a RuntimeContext argument");
                        }
                    } else {
                        if (constructors.length == 0) {
                            throw new FunctionClassInstantiationException("The function " + getMethod().getTargetClass() + " cannot be instantiated as it has no public constructors. Function classes should have a single public constructor that takes either no arguments or a RuntimeContext argument");
                        } else {
                            throw new FunctionClassInstantiationException("The function " + getMethod().getTargetClass() + " cannot be instantiated as it has multiple public constructors. Function classes should have a single public constructor that takes either no arguments or a RuntimeContext argument");

                        }
                    }
                } catch (InvocationTargetException e) {
                    throw new FunctionClassInstantiationException("An error occurred in the function constructor while instantiating " + getMethod().getTargetClass(), e.getCause());
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new FunctionClassInstantiationException("The function class " + getMethod().getTargetClass() + " could not be instantiated", e);
                }
            }
            return Optional.of(instance);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getConfigurationByKey(String key) {
        return Optional.ofNullable(config.get(key));
    }

    @Override
    public Map<String, String> getConfiguration() {
        return config;
    }

    @Override
    public <T> Optional<T> getAttribute(String att, Class<T> type) {
        Objects.requireNonNull(att);
        Objects.requireNonNull(type);

        return Optional.ofNullable(type.cast(attributes.get(att)));
    }

    @Override
    public void setAttribute(String att, Object val) {
        Objects.requireNonNull(att);

        attributes.put(att, val);
    }

    @Override
    public void addInputCoercion(InputCoercion ic) {
        userInputCoercions.add(Objects.requireNonNull(ic));
    }

    /**
     * Gets a stream of the possible {@link InputCoercion} for this parameter.
     * <p>
     * If the parameter has been annotated with a specific coercion, only that coercion is
     * tried, otherwise configuration-provided coercions are tried first and builtin
     * coercions second.
     *
     * @param targetMethod The user function method
     * @param param        The index of the parameter
     */
    List<InputCoercion> getInputCoercions(Method targetMethod, int param) {
        Annotation parameterAnnotations[] = targetMethod.getParameterAnnotations()[param];
        Optional<Annotation> coercionAnnotation = Arrays.stream(parameterAnnotations)
                .filter((ann) -> ann.annotationType().equals(InputBinding.class))
                .findFirst();
        if (coercionAnnotation.isPresent()) {
            try {
                List<InputCoercion> coercionList = new ArrayList<InputCoercion>();
                InputBinding inputBindingAnnotation = (InputBinding) coercionAnnotation.get();
                coercionList.add(inputBindingAnnotation.coercion().newInstance());
                return coercionList;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new FunctionInputHandlingException("Unable to instantiate input coercion class for argument " + param + " of " + targetMethod);
            }
        }
        List<InputCoercion> inputList = new ArrayList<>();
        inputList.addAll(userInputCoercions);
        inputList.addAll(builtinInputCoercions);
        return inputList;
    }

    @Override
    public void addOutputCoercion(OutputCoercion oc) {
        userOutputCoercions.add(Objects.requireNonNull(oc));
    }

    @Override
    public void setInvoker(FunctionInvoker invoker) {
        // TODO: Decide whether to remove default invoker, or simply let this invoker take precedence
        configuredInvokers.add(1, invoker);
    }

    @Override
    public MethodWrapper getMethod() {
        return method;
    }

    public FunctionInvocationContext newInvocationContext() {
        return new FunctionInvocationContext(this);
    }

    public OutputEvent tryInvoke(InputEvent evt, InvocationContext entryPoint) {
        OutputEvent output = null;
        for (FunctionInvoker invoker : configuredInvokers) {
            Optional<OutputEvent> result = invoker.tryInvoke(entryPoint, evt);
            if (result.isPresent()) {
                output = result.get();
                break;
            }
        }
        return output;
    }

    /**
     * Gets a stream of the possible {@link OutputCoercion} for the method.
     * <p>
     * If the method has been annotated with a specific coercion, only that coercion is
     * tried, otherwise configuration-provided coercions are tried first and builtin
     * coercions second.
     *
     * @param method The user function method
     */
    List<OutputCoercion> getOutputCoercions(Method method) {
        OutputBinding coercionAnnotation = method.getAnnotation(OutputBinding.class);
        if (coercionAnnotation != null) {
            try {
                List<OutputCoercion> coercionList = new ArrayList<OutputCoercion>();
                coercionList.add(coercionAnnotation.coercion().newInstance());
                return coercionList;

            } catch (IllegalAccessException | InstantiationException e) {
                throw new FunctionConfigurationException("Unable to instantiate output coercion class for method " + getMethod());
            }
        }
        List<OutputCoercion> outputList = new ArrayList<OutputCoercion>();
        outputList.addAll(userOutputCoercions);
        outputList.addAll(builtinOutputCoercions);
        return outputList;
    }

    public MethodWrapper getMethodWrapper() {
        return method;
    }
}
