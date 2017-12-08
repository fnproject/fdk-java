package com.fnproject.fn.api;


import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Captures the context of a function runtime's lifecycle.
 * <p>
 * The attributes represented by this interface are constant over the lifetime
 * of a function; they will not change between multiple invocations of a hot function.
 */
public interface RuntimeContext {
    /**
     * Create an instance of the user specified class on which the target function to invoke is declared.
     *
     * @return new instance of class containing the target function
     */
    Optional<Object> getInvokeInstance();

    /**
     * Get the target method of the user specified function wrapped in a {@link MethodWrapper}.
     *
     * @return the target method of the function invocation
     */
    MethodWrapper getMethod();

    /**
     * Get a configuration variable value by key
     *
     * @param key the name of the configuration variable
     * @return an Optional String value of the config variable
     */
    Optional<String> getConfigurationByKey(String key);

    /**
     * Get the configuration variables associated with this com.fnproject.fn.runtime
     *
     * @return an immutable map of configuration variables
     */
    Map<String, String> getConfiguration();

    /**
     * Get an attribute from the context.
     *
     * @param att  the attribute ID
     * @param type the type of the attribute
     * @param <T>  the type of the attribute
     * @return an Optional which contains the attribute if it is set.
     */
    <T> Optional<T> getAttribute(String att, Class<T> type);

    /**
     * Set an attribute , overwriting any previous value
     *
     * @param att the attribute name
     * @param val nullable attribute to set
     */
    void setAttribute(String att, Object val);

    /**
     * Add an {@link InputCoercion}. {@link InputCoercion} instances added here will be
     * tried in order, and before any of the built-in {@link InputCoercion} are tried.
     *
     * @param ic The {@link InputCoercion} to add
     */
    void addInputCoercion(InputCoercion ic);

    /**
     * Gets a list of the possible {@link InputCoercion} for this parameter.
     * <p>
     * If the parameter has been annotated with a specific coercion, only that coercion is
     * tried, otherwise configuration-provided coercions are tried first and builtin
     * coercions second.
     *
     * @param targetMethod The user function method
     * @param param        The index of the parameter
     * @return  a list of configured input coercions to apply to the given parameter
     */
    List<InputCoercion> getInputCoercions(MethodWrapper targetMethod, int param);

    /**
     * Add an {@link OutputCoercion}. {@link OutputCoercion} instances added here will be
     * tried in order, before any of the builtin {@link OutputCoercion} are tried.
     *
     * @param oc The {@link OutputCoercion} to add
     */
    void addOutputCoercion(OutputCoercion oc);

    /**
     * Gets a list of the possible {@link OutputCoercion} for the method.
     * <p>
     * If the method has been annotated with a specific coercion, only that coercion is
     * tried, otherwise configuration-provided coercions are tried first and builtin
     * coercions second.
     *
     * @param method The user function method
     * @return a list of configured output coercions to apply to the given method.
     */
    List<OutputCoercion> getOutputCoercions(Method method);

    /**
     * Set an {@link FunctionInvoker} for this function. The invoker will override
     * the built in function invoker, although the cloud threads invoker will still
     * have precedence so that cloud threads can be used from functions using custom invokers.
     * @param invoker The {@link FunctionInvoker} to add.
     */
    void setInvoker(FunctionInvoker invoker);
}
