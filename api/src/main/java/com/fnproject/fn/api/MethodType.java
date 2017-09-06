package com.fnproject.fn.api;

/**
 * Interface representing type information about a method, possibly a parameter or return type.
 */
public interface MethodType {
    /**
     * Unlike {@link java.lang.reflect.Method}, types (such as parameter types, or return types) are
     * resolved to a reified type even if they are generic, providing reification is possible.
     *
     * For example, take the following classes:
     * <pre>{@code
     * class GenericParent&lt;T&gt; {
     *   public void someMethod(T t) { // do something with t }
     * }
     *
     * class ConcreteClass extends GenericParent&lt;String&gt; { }
     * }</pre>
     *
     * A {@link MethodType} representing the first argument of {@code someMethod} would return {@code String.class}
     * instead of {@code Object.class}
     *
     * @return Reified type
     */
    Class<?> getParameterClass();
}
