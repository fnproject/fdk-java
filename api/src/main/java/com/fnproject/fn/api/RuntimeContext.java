package com.fnproject.fn.api;


import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Captures the context of a function runtime's lifecycle.
 * <p>
 * The attributes represented by this interface are constant over the lifetime
 * of a function; they will not change between multiple invocations of a hot function.
 */
public interface RuntimeContext {
    Optional<Object> getInvokeInstance();

    /**
     * Get the target class for the function invocation
     *
     * @return the class the user has configured as the function entrypoint
     */
    Class<?> getTargetClass();

    /**
     * Get the target method of the function invocation
     *
     * @return the target method of the function invocation
     */
    Method getTargetMethod();

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
     * get an attribute from the context.
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
     * Add an {@link OutputCoercion}. {@link OutputCoercion} instances added here will be
     * tried in order, before any of the builtin {@link OutputCoercion} are tried.
     *
     * @param oc The {@link OutputCoercion} to add
     */
    void addOutputCoercion(OutputCoercion oc);

    /**
     * Set an {@link FunctionInvoker} for this function. The invoker will override
     * the built in function invoker, although the cloud threads invoker will still
     * have precedence so that cloud threads can be used from functions using custom invokers.
     * @param invoker The {@link FunctionInvoker} to add.
     */
    void setInvoker(FunctionInvoker invoker);
}
